package com.myfitness.api.common.config;

import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientIpResolverTest {
    private ClientIpResolver resolver(String cidrs) {
        return new ClientIpResolver(cidrs, new MockEnvironment());
    }

    private MockHttpServletRequest request(String peer, String header) {
        var request = new MockHttpServletRequest();
        request.setRemoteAddr(peer);
        if (header != null) request.addHeader("X-Forwarded-For", header);
        return request;
    }

    @Test void defaultIgnoresAllForwardingHeaders() {
        var request = request("192.0.2.1", "malformed");
        request.addHeader("Forwarded", "for=198.51.100.1");
        request.addHeader("X-Real-IP", "198.51.100.2");
        assertThat(resolver("").resolve(request)).isEqualTo("192.0.2.1");
    }

    @Test void trustedProxyUsesNumericClientAndIgnoresOtherHeaderFamilies() {
        var request = request("10.20.0.2", "198.51.100.7");
        request.addHeader("Forwarded", "for=198.51.100.88");
        request.addHeader("X-Real-IP", "198.51.100.99");
        assertThat(resolver("10.20.0.0/24").resolve(request)).isEqualTo("198.51.100.7");
    }

    @Test void attackerControlledLeftPrefixCannotReplaceTheActualClient() {
        var resolver = resolver("10.20.0.0/24, 2001:db8:ffff::/48");
        assertThat(resolver.resolve(request("10.20.0.2", "203.0.113.99, 198.51.100.7, 2001:db8:ffff::2")))
            .isEqualTo("198.51.100.7");
        assertThat(resolver.resolve(request("10.20.0.3", "203.0.113.98, 198.51.100.7")))
            .isEqualTo("198.51.100.7");
    }

    @Test void untrustedIntermediateIsTheBoundaryEvenIfItClaimsAnotherClient() {
        assertThat(resolver("10.20.0.0/24").resolve(request("10.20.0.2", "203.0.113.9, 192.0.2.9")))
            .isEqualTo("192.0.2.9");
    }

    @Test void ipv6AndMappedIpv4AreCanonicalized() {
        var resolver = resolver("10.20.0.2,2001:db8:ffff::/48");
        assertThat(resolver.resolve(request("2001:db8:ffff::1", "2001:db8::7")))
            .isEqualTo(resolver.resolve(request("10.20.0.2", "2001:0db8:0:0:0:0:0:7")));
        assertThat(resolver.resolve(request("::ffff:192.0.2.1", null)))
            .isEqualTo(resolver.resolve(request("192.0.2.1", null)));
        assertThat(resolver.resolve(request("::ffff:10.20.0.2", "198.51.100.7"))).isEqualTo("198.51.100.7");
    }

    @Test void ipv4CidrBoundariesAreExact() {
        var resolver = resolver("10.20.0.4/30");
        assertThat(resolver.resolve(request("10.20.0.4", "198.51.100.1"))).isEqualTo("198.51.100.1");
        assertThat(resolver.resolve(request("10.20.0.7", "198.51.100.1"))).isEqualTo("198.51.100.1");
        assertThat(resolver.resolve(request("10.20.0.3", "198.51.100.1"))).isEqualTo("10.20.0.3");
        assertThat(resolver.resolve(request("10.20.0.8", "198.51.100.1"))).isEqualTo("10.20.0.8");
    }

    @Test void ipv6CidrBoundariesAndHostEntriesAreExact() {
        var resolver = resolver("2001:db8::4/126");
        assertThat(resolver.resolve(request("2001:db8::4", "198.51.100.1"))).isEqualTo("198.51.100.1");
        assertThat(resolver.resolve(request("2001:db8::7", "198.51.100.1"))).isEqualTo("198.51.100.1");
        assertThat(resolver.resolve(request("2001:db8::3", "198.51.100.1"))).isNotEqualTo("198.51.100.1");
        assertThat(resolver.resolve(request("2001:db8::8", "198.51.100.1"))).isNotEqualTo("198.51.100.1");
        assertThat(resolver("2001:db8::1/128").resolve(request("2001:db8::2", "198.51.100.1")))
            .isNotEqualTo("198.51.100.1");
        assertThat(resolver("10.20.0.2/32").resolve(request("10.20.0.3", "198.51.100.1"))).isEqualTo("10.20.0.3");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "unknown", "_hidden", "example.com", "192.0.2.1:80", "[2001:db8::1]",
        "fe80::1%eth0", "for=192.0.2.1", "192.0.2.999", "192.0.2", "001.2.3.4", "192.0.2.1,",
        ",192.0.2.1", "192.0.2.1,,198.51.100.1", "2001:db8:::1", "::ffff:001.2.3.4", "192.0.2.1\r\nInjected: yes"})
    void malformedTrustedHeaderIsRejectedNotUsedAsANewQuota(String value) {
        assertThatThrownBy(() -> resolver("10.20.0.2").resolve(request("10.20.0.2", value)))
            .isInstanceOf(ClientIpResolver.InvalidForwardingHeaderException.class);
        assertThat(resolver("10.20.0.2").resolve(request("192.0.2.1", value))).isEqualTo("192.0.2.1");
    }

    @Test void duplicateOversizedTooManyHopsAndNoUntrustedBoundaryAreRejected() {
        var resolver = resolver("10.20.0.2");
        var duplicate = request("10.20.0.2", "192.0.2.1");
        duplicate.addHeader("X-Forwarded-For", "192.0.2.2");
        assertThatThrownBy(() -> resolver.resolve(duplicate)).isInstanceOf(ClientIpResolver.InvalidForwardingHeaderException.class);
        for (String value : new String[]{"1".repeat(2049), "192.0.2.1,".repeat(32) + "192.0.2.1", "10.20.0.2"}) {
            assertThatThrownBy(() -> resolver.resolve(request("10.20.0.2", value)))
                .isInstanceOf(ClientIpResolver.InvalidForwardingHeaderException.class);
        }
        assertThat(resolver.resolve(request("10.20.0.2", null))).isEqualTo("10.20.0.2");
    }

    @ParameterizedTest
    @ValueSource(strings = {"*", "0.0.0.0/0", "::/0", "10.0.0.1/33", "2001:db8::/129", "proxy.internal",
        "10.0.0.1,", ",10.0.0.1", "10.0.0.1/-1", "10.0.0.1/foo", "10.0.0.1/", "10.0.0.1/32/1", "127.1",
        "::ffff:192.0.2.1/128"})
    void invalidTrustConfigurationFailsStartup(String value) {
        assertThatThrownBy(() -> resolver(value)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void excessiveTrustConfigurationFailsStartup() {
        assertThatThrownBy(() -> resolver("10.20.0.2,".repeat(64) + "10.20.0.3"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"native", "framework", "", "invalid"})
    void conflictingForwardHeaderStrategyFailsStartup(String strategy) {
        assertThatThrownBy(() -> new ClientIpResolver("", new MockEnvironment()
                .withProperty("server.forward-headers-strategy", strategy)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void invalidRemoteAddressSharesConservativeUnknownBucket() {
        assertThat(resolver("").resolve(request(null, "192.0.2.1"))).isEqualTo("unknown");
        assertThat(resolver("").resolve(request("attacker.invalid", "192.0.2.1"))).isEqualTo("unknown");
    }
}
