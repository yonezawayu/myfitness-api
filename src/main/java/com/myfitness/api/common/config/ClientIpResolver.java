package com.myfitness.api.common.config;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** Resolves numeric client addresses without trusting caller-supplied proxy identities. */
@Component
public class ClientIpResolver {
    private static final int MAX_HOPS = 32;
    private static final int MAX_HEADER_LENGTH = 2048;
    private final List<Network> proxies;

    public ClientIpResolver(@Value("${myfitness.proxy.trusted-cidrs:}") String configured,
                            Environment environment) {
        // A container/filter must not rewrite the socket peer before we establish trust.
        if (!"none".equalsIgnoreCase(environment.getProperty("server.forward-headers-strategy", "none").trim())) {
            throw new IllegalArgumentException("Client IP resolution requires forward-headers-strategy=none");
        }
        var networks = new ArrayList<Network>();
        if (!configured.isBlank()) {
            String[] entries = configured.split(",", -1);
            if (entries.length > 64) throw invalidConfiguration();
            for (String entry : entries) networks.add(Network.parse(entry.trim()));
        }
        proxies = List.copyOf(networks);
    }

    public String resolve(HttpServletRequest request) {
        Ip peer = Ip.parse(request.getRemoteAddr());
        if (peer == null) return "unknown";
        // Ignore ALL forwarding headers from an untrusted peer, even malformed ones.
        if (!trusted(peer)) return peer.text();
        var values = request.getHeaders("X-Forwarded-For");
        if (values == null || !values.hasMoreElements()) return peer.text();
        String value = values.nextElement();
        if (values.hasMoreElements() || value == null || value.isBlank() || value.length() > MAX_HEADER_LENGTH) {
            throw new InvalidForwardingHeaderException();
        }
        String[] tokens = value.split(",", -1);
        if (tokens.length > MAX_HOPS) throw new InvalidForwardingHeaderException();
        var chain = new ArrayList<Ip>();
        for (String token : tokens) {
            Ip address = Ip.parse(token.trim());
            if (address == null) throw new InvalidForwardingHeaderException();
            chain.add(address);
        }
        // The trusted ingress must append its actual peer (or overwrite with a verified chain).
        // Stop at the first UNTRUSTED hop from the right; never use the leftmost value blindly.
        Ip current = peer;
        for (int i = chain.size() - 1; i >= 0; i--) {
            if (!trusted(current)) return current.text();
            current = chain.get(i);
        }
        if (trusted(current)) throw new InvalidForwardingHeaderException();
        return current.text();
    }

    private boolean trusted(Ip address) {
        return proxies.stream().anyMatch(network -> network.contains(address));
    }

    private static IllegalArgumentException invalidConfiguration() {
        return new IllegalArgumentException("Invalid trusted proxy IP/CIDR configuration");
    }

    public static final class InvalidForwardingHeaderException extends IllegalArgumentException {
        InvalidForwardingHeaderException() { super("Invalid forwarding header"); }
    }

    private record Ip(byte[] bytes, String text) {
        static Ip parse(String value) {
            if (value == null || value.isEmpty() || value.length() > 64
                    || !value.matches("[0-9a-fA-F:.]+")) return null;
            if (!value.contains(":")) {
                String[] octets = value.split("\\.", -1);
                if (octets.length != 4) return null;
                byte[] bytes = new byte[4];
                for (int i = 0; i < 4; i++) {
                    if (!octets[i].matches("0|[1-9][0-9]{0,2}")) return null;
                    int number = Integer.parseInt(octets[i]);
                    if (number > 255) return null;
                    bytes[i] = (byte) number;
                }
                return new Ip(bytes, value);
            }
            if (value.contains(".") && Ip.parse(value.substring(value.lastIndexOf(':') + 1)) == null) {
                return null;
            }
            try {
                // ':' and the numeric-only whitelist exclude hostnames, zones, brackets and ports.
                InetAddress parsed = InetAddress.getByName(value);
                return new Ip(parsed.getAddress(), parsed.getHostAddress());
            } catch (UnknownHostException exception) {
                return null;
            }
        }
    }

    private record Network(byte[] address, int prefix) {
        static Network parse(String input) {
            String[] parts = input.split("/", -1);
            if (parts.length > 2) throw invalidConfiguration();
            Ip ip = Ip.parse(parts[0]);
            if (ip == null) throw invalidConfiguration();
            int bits = ip.bytes().length * 8;
            int prefix = bits;
            if (parts.length == 2) {
                if (!parts[1].matches("[0-9]{1,3}")) throw invalidConfiguration();
                prefix = Integer.parseInt(parts[1]);
            }
            // Never accept a catch-all network. IP-mapped IPv6 uses IPv4's 32-bit range.
            if (prefix < 1 || prefix > bits) throw invalidConfiguration();
            return new Network(Arrays.copyOf(ip.bytes(), ip.bytes().length), prefix);
        }

        boolean contains(Ip ip) {
            byte[] candidate = ip.bytes();
            if (candidate.length != address.length) return false;
            for (int bit = 0; bit < prefix; bit++) {
                int mask = 1 << (7 - bit % 8);
                if ((candidate[bit / 8] & mask) != (address[bit / 8] & mask)) return false;
            }
            return true;
        }
    }
}
