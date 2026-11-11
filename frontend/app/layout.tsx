import type { Metadata } from "next";
import { connection } from "next/server";
import "./globals.css";

export const metadata: Metadata = {
  title: "MyFitness API",
  description: "Portfolio frontend for MyFitness API"
};

export default async function RootLayout({
  children
}: Readonly<{
  children: React.ReactNode;
}>) {
  if (process.env.NODE_ENV === "production") {
    // Nonces are only available at request time; do not prerender cached HTML.
    await connection();
  }
  return (
    <html lang="ja">
      <body>{children}</body>
    </html>
  );
}
