# SAMI Security Auditor

An advanced Android web security auditor and vulnerability scanner built with modern Kotlin and Jetpack Compose, rewritten from the original SAMI Scanner.

## Features

- **Real-Time Security Audit**:
  - **Target Summary**: Analyzes target URL, latency, response code, and verifies SSL/TLS HTTPS encryption status.
  - **Security Headers & Hardening**: Evaluates critical browser hardening headers (`Content-Security-Policy`, `Strict-Transport-Security`, `X-Frame-Options`, `X-Content-Type-Options`, `Referrer-Policy`, and `Permissions-Policy`) with comprehensive explanations in English and Arabic.
  - **Technology & Information Disclosure**: Detects exposed server software banners (`Server`), backend runtime leaks (`X-Powered-By`), and misconfigured CORS policies (such as wildcard `*`).
  - **Cookie Security Flags**: Checks for `HttpOnly`, `Secure`, and `SameSite` flags on cookies.
  - **Parallel Sensitive Endpoints Recon**: Concurrently probes common administrative and sensitive paths (`admin`, `login`, `.env`, `config.php`, `robots.txt`, `.git/HEAD`, `sitemap.xml`, `backup.sql`) without following redirects.
- **Security Score & Posture Summary**:
  - Instant security grade (`A`, `B`, `C`, `F`) and 0-100 rating based on hardening checks.
  - Quick count badges for Passed, Warnings, and Critical findings.
- **Live Console Streaming**:
  - Real-time streaming log terminal with color-coded severity tags (`[+]`, `[-]`, `[!]`, `[*]`) matching the original scanner's audit feed.
- **Reporting & Exporting**:
  - Save report directly to app internal storage.
  - One-tap Android system share dialog (`Intent.ACTION_SEND`).
  - Copy full text report to clipboard.
- **Cybersecurity Dark Theme**:
  - Sleek dark slate and gold cybersecurity aesthetic strictly following Material Design 3.
  - Fully responsive, accessible, and edge-to-edge layout.

## Tech Stack

- **Platform**: Android (minSdk 26, targetSdk 36)
- **Language**: Kotlin 2.2
- **UI Framework**: Jetpack Compose with Material 3
- **Networking & Concurrency**: OkHttp 4.12 & Kotlin Coroutines
- **Build System**: Gradle 9.3 & Android Gradle Plugin 9.1
