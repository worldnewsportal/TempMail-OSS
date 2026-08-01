package com.example.data.provider

import java.util.UUID

class SimulationProvider : EmailProvider {
    override val providerName: String = "Premium (Simulation)"

    val domains = listOf(
        "tempinbox.premium", "securesmtp.vip", "privacyinbox.top", "fastmail.tech", "shieldmail.club",
        "tempmail.cloud", "inboxsafe.online", "anonymbox.space", "disposable.website", "hushmail.pro",
        "trashmail.today", "ghostmail.co", "ninjaemail.app", "stealthinbox.me", "airmail.site",
        "rapidmail.icu", "quickinbox.xyz", "easytemp.info", "freetemp.net", "instantmail.work",
        "zerospam.dev", "cleaninbox.live", "vaultmail.pw", "safehost.cc", "guardmail.biz",
        "shieldid.email", "privatebox.us", "no-spam.care", "directtemp.net", "safesmtp.eu",
        "hidemail.co.uk", "maskinbox.ca", "fakeinbox.de", "dummymail.fr", "trashcan.io",
        "throwaway.org", "byemail.net", "incognito.host", "cloakmail.best", "vortexmail.xyz",
        "nomadmail.cc", "tempaddress.com", "mytrashmail.co", "spamshield.org", "blackhole.mail",
        "shadowmail.net", "phantombox.com", "fortressmail.info", "bunkermail.net", "oasismail.org",
        "miragemail.net", "havenmail.com", "sanctuarymail.net", "zenmailbox.com", "auroramail.net"
    )

    override suspend fun healthCheck(): Boolean = true

    override suspend fun getAvailableDomains(): List<String> = domains

    override suspend fun createAccount(
        customUsername: String?,
        domain: String?
    ): ProviderAccountResult {
        val selectedDomain = if (!domain.isNullOrBlank() && domains.contains(domain)) {
            domain
        } else {
            domains.first()
        }
        val username = if (!customUsername.isNullOrBlank()) {
            customUsername.lowercase().replace(Regex("[^a-z0-9]"), "")
        } else {
            "premium" + UUID.randomUUID().toString().replace("-", "").take(8)
        }

        val fullAddress = "$username@$selectedDomain"
        return ProviderAccountResult(
            id = "sim_${UUID.randomUUID()}",
            address = fullAddress,
            username = username,
            domain = selectedDomain,
            token = "sim_token_${System.currentTimeMillis()}",
            providerName = providerName
        )
    }

    override suspend fun fetchInbox(
        accountId: String,
        token: String
    ): List<ProviderMessageSummary> {
        val accountCreatedTime = try {
            token.removePrefix("sim_token_").toLong()
        } catch (e: Exception) {
            System.currentTimeMillis() - 60000L
        }

        val now = System.currentTimeMillis()
        val timeElapsed = now - accountCreatedTime

        val list = mutableListOf<ProviderMessageSummary>()

        // 1. Welcome Message (Always present immediately)
        list.add(
            ProviderMessageSummary(
                id = "${accountId}_welcome",
                fromName = "TempMail Premium Support",
                fromEmail = "support@tempinbox.premium",
                subject = "Welcome to your Premium Temporary Mailbox! 🚀",
                preview = "Enjoy unlimited high-speed disposable email addresses on over 50+ exclusive domains with zero spam.",
                receivedAt = accountCreatedTime,
                hasAttachments = true
            )
        )

        // 2. Google Verification Code (Appears after 5 seconds)
        if (timeElapsed >= 5000L) {
            list.add(
                ProviderMessageSummary(
                    id = "${accountId}_google_verify",
                    fromName = "Google Accounts",
                    fromEmail = "noreply@google.com",
                    subject = "Google Verification Code: 481029",
                    preview = "Verify your account. If you didn't request this code, ignore this email.",
                    receivedAt = accountCreatedTime + 5000L,
                    hasAttachments = false
                )
            )
        }

        // 3. GitHub Login Alert (Appears after 15 seconds)
        if (timeElapsed >= 15000L) {
            list.add(
                ProviderMessageSummary(
                    id = "${accountId}_github_login",
                    fromName = "GitHub Security",
                    fromEmail = "noreply@github.com",
                    subject = "[GitHub] Security Alert: New login from unknown device",
                    preview = "We detected a new login to your account from Chrome on Linux (IP: 198.51.100.42). Please verify.",
                    receivedAt = accountCreatedTime + 15000L,
                    hasAttachments = false
                )
            )
        }

        // 4. Netflix Subscription Offer (Appears after 35 seconds)
        if (timeElapsed >= 35000L) {
            list.add(
                ProviderMessageSummary(
                    id = "${accountId}_netflix_promo",
                    fromName = "Netflix VIP",
                    fromEmail = "info@netflix.com",
                    subject = "Ready to watch? Complete your Netflix sign-up today! 🍿",
                    preview = "Unlimited movies, TV shows, and more. Watch anywhere. Cancel anytime.",
                    receivedAt = accountCreatedTime + 35000L,
                    hasAttachments = false
                )
            )
        }

        // 5. Weekly newsletter with attachments (Appears after 60 seconds)
        if (timeElapsed >= 60000L) {
            list.add(
                ProviderMessageSummary(
                    id = "${accountId}_newsletter",
                    fromName = "Tech Weekly",
                    fromEmail = "newsletter@techweekly.xyz",
                    subject = "Tech Weekly #142: The future of Jetpack Compose and AI Developers",
                    preview = "Download this week's full digital PDF report inside. Explore how modern declarative UI is accelerating App Store shipping.",
                    receivedAt = accountCreatedTime + 60000L,
                    hasAttachments = true
                )
            )
        }

        // Return sorted by newest first
        return list.sortedByDescending { it.receivedAt }
    }

    override suspend fun fetchMessageDetails(
        accountId: String,
        token: String,
        messageId: String
    ): ProviderMessageDetail {
        val accountCreatedTime = try {
            token.removePrefix("sim_token_").toLong()
        } catch (e: Exception) {
            System.currentTimeMillis() - 60000L
        }

        return when {
            messageId.endsWith("_welcome") -> {
                ProviderMessageDetail(
                    id = messageId,
                    fromName = "TempMail Premium Support",
                    fromEmail = "support@tempinbox.premium",
                    subject = "Welcome to your Premium Temporary Mailbox! 🚀",
                    textBody = "Welcome to TempMail Premium! You are currently using one of our exclusive static simulation domains. We provide over 50+ beautiful domains to prevent website blocking.\n\nEnjoy unlimited high-speed disposable email addresses on over 50+ exclusive domains with zero spam. Your mailbox remains completely active as long as you keep the app running.",
                    htmlBody = """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta charset="utf-8">
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                            <style>
                                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; line-height: 1.6; color: #1E293B; background: #F8FAFC; padding: 24px; margin: 0; }
                                .container { max-width: 600px; margin: 0 auto; background: #FFFFFF; border-radius: 16px; border: 1px solid #E2E8F0; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05); }
                                .header { background: linear-gradient(135deg, #4F46E5 0%, #7C3AED 100%); color: #FFFFFF; padding: 32px 24px; text-align: center; }
                                .header h1 { margin: 0; font-size: 24px; font-weight: 800; letter-spacing: -0.025em; }
                                .content { padding: 32px 24px; }
                                .content p { margin: 0 0 16px; color: #475569; font-size: 16px; }
                                .badge { display: inline-block; background: #EEF2F6; color: #4F46E5; font-weight: 700; padding: 6px 12px; border-radius: 9999px; font-size: 13px; margin-bottom: 24px; }
                                .features { background: #F8FAFC; border-radius: 12px; padding: 20px; border: 1px solid #E2E8F0; margin-bottom: 24px; }
                                .features h3 { margin: 0 0 12px; color: #1E293B; font-size: 16px; font-weight: 700; }
                                .features ul { margin: 0; padding-left: 20px; color: #475569; font-size: 14px; }
                                .features li { margin-bottom: 8px; }
                                .footer { text-align: center; padding: 24px; font-size: 12px; color: #94A3B8; border-top: 1px solid #F1F5F9; }
                            </style>
                        </head>
                        <body>
                            <div class="container">
                                <div class="header">
                                    <h1>TempMail Premium</h1>
                                </div>
                                <div class="content">
                                    <span class="badge">SIMULATION MODE ACTIVE</span>
                                    <p>Hello,</p>
                                    <p>Thank you for choosing <b>TempMail Premium</b>. Your temporary secure email account is fully active and ready to receive messages.</p>
                                    <div class="features">
                                        <h3>Why use our Premium Domains?</h3>
                                        <ul>
                                            <li><b>Over 50+ Premium Domains:</b> Bypass blocking systems on sites like GitHub, Google, Amazon, and Netflix easily.</li>
                                            <li><b>100% Secure &amp; Offline-Ready:</b> All downloaded messages are safely saved on your local device.</li>
                                            <li><b>Fully Interactive:</b> Supports HTML email rendering, dark mode, attachments, and search.</li>
                                        </ul>
                                    </div>
                                    <p>Simply copy your address, register anywhere, and see verification codes appear instantly inside the app.</p>
                                </div>
                                <div class="footer">
                                    &copy; 2026 TempMail OSS. Distributed under MIT Open Source License.
                                </div>
                            </div>
                        </body>
                        </html>
                    """.trimIndent(),
                    receivedAt = accountCreatedTime,
                    attachments = listOf(
                        ProviderAttachment(
                            id = "${messageId}_att_pdf",
                            filename = "User_Guide_Premium_v2.pdf",
                            contentType = "application/pdf",
                            sizeBytes = 2048500L,
                            downloadUrl = "https://example.com/guide.pdf"
                        ),
                        ProviderAttachment(
                            id = "${messageId}_att_png",
                            filename = "Premium_Welcome_Art.png",
                            contentType = "image/png",
                            sizeBytes = 458900L,
                            downloadUrl = "https://example.com/welcome.png"
                        )
                    )
                )
            }
            messageId.endsWith("_google_verify") -> {
                ProviderMessageDetail(
                    id = messageId,
                    fromName = "Google Accounts",
                    fromEmail = "noreply@google.com",
                    subject = "Google Verification Code: 481029",
                    textBody = "Hi!\n\nSomeone is trying to verify your Google Account registration using this temporary address. If you requested this code, enter it in your browser:\n\n481029\n\nThis code expires in 15 minutes.\n\nThank you,\nGoogle Team",
                    htmlBody = """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <style>
                                body { font-family: Roboto, sans-serif; line-height: 1.5; color: #202124; background-color: #ffffff; padding: 20px; }
                                .container { border: 1px solid #dadce0; border-radius: 8px; max-width: 500px; margin: 0 auto; padding: 40px 30px; }
                                .logo { text-align: center; margin-bottom: 30px; }
                                .code { font-size: 32px; font-weight: bold; letter-spacing: 6px; color: #1a73e8; text-align: center; margin: 30px 0; background: #f8f9fa; padding: 15px; border-radius: 4px; border: 1px dashed #dadce0; }
                                .footer { font-size: 12px; color: #5f6368; margin-top: 30px; border-top: 1px solid #f1f3f4; padding-top: 20px; text-align: center; }
                            </style>
                        </head>
                        <body>
                            <div class="container">
                                <div class="logo">
                                    <h2 style="color: #4285F4; margin: 0; font-family: Google Sans, Roboto, sans-serif;">Google Account</h2>
                                </div>
                                <p>Hi,</p>
                                <p>Thank you for verifying your Google account with us. Please use the verification code below to complete registration:</p>
                                <div class="code">481029</div>
                                <p>If you did not request this verification code, please ignore this email. Your privacy and security are completely safe.</p>
                                <div class="footer">
                                    Google LLC, 1600 Amphitheatre Parkway, Mountain View, CA 94043
                                </div>
                            </div>
                        </body>
                        </html>
                    """.trimIndent(),
                    receivedAt = accountCreatedTime + 5000L
                )
            }
            messageId.endsWith("_github_login") -> {
                ProviderMessageDetail(
                    id = messageId,
                    fromName = "GitHub Security",
                    fromEmail = "noreply@github.com",
                    subject = "[GitHub] Security Alert: New login from unknown device",
                    textBody = "Hey there!\n\nWe detected a new login to your account using the device Chrome on Linux (IP: 198.51.100.42).\n\nIf this was you, you're all set! If this was not you, please secure your account immediately.\n\nGitHub Support Team",
                    htmlBody = """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <style>
                                body { font-family: -apple-system, sans-serif; line-height: 1.5; color: #24292e; padding: 20px; background-color: #f6f8fa; }
                                .card { max-width: 550px; margin: 0 auto; background: #ffffff; border: 1px solid #e1e4e8; border-radius: 6px; padding: 32px; }
                                .header { margin-bottom: 24px; border-bottom: 1px solid #e1e4e8; padding-bottom: 16px; }
                                .alert-box { background-color: #fffbdd; border: 1px solid #d9d0a5; padding: 16px; border-radius: 6px; margin-bottom: 20px; font-size: 14px; }
                                .footer { font-size: 12px; color: #586069; margin-top: 30px; text-align: center; }
                            </style>
                        </head>
                        <body>
                            <div class="card">
                                <div class="header">
                                    <h3 style="margin:0; font-size: 20px; font-weight: 600;">GitHub Security</h3>
                                </div>
                                <p>We noticed a new login on your GitHub account:</p>
                                <div class="alert-box">
                                    <b>Device:</b> Chrome on Linux<br/>
                                    <b>IP:</b> 198.51.100.42 (Dublin, Ireland)<br/>
                                    <b>Time:</b> Just now
                                </div>
                                <p>If this was indeed you, no action is needed. If you do not recognize this login, please update your passwords and security settings immediately.</p>
                                <div class="footer">
                                    GitHub, Inc. &bull; 88 Colin P Kelly Jr St &bull; San Francisco, CA 94107
                                </div>
                            </div>
                        </body>
                        </html>
                    """.trimIndent(),
                    receivedAt = accountCreatedTime + 15000L
                )
            }
            messageId.endsWith("_netflix_promo") -> {
                ProviderMessageDetail(
                    id = messageId,
                    fromName = "Netflix VIP",
                    fromEmail = "info@netflix.com",
                    subject = "Ready to watch? Complete your Netflix sign-up today! 🍿",
                    textBody = "Hi!\n\nUnlimited movies, TV shows, and more. Watch anywhere. Cancel anytime.\n\nComplete your sign-up today and unlock exclusive premium plans starting at ${"$"}/mo.\n\nEnjoy Netflix!",
                    htmlBody = """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <style>
                                body { font-family: Helvetica, Arial, sans-serif; line-height: 1.4; background-color: #000000; color: #ffffff; padding: 20px; }
                                .container { max-width: 500px; margin: 0 auto; background: #141414; padding: 40px; border-radius: 8px; text-align: center; border: 1px solid #222; }
                                .logo { font-size: 36px; font-weight: bold; color: #E50914; font-family: Impact, sans-serif; margin-bottom: 30px; letter-spacing: 2px; }
                                .btn { display: inline-block; background-color: #E50914; color: #ffffff; text-decoration: none; padding: 14px 28px; font-weight: bold; border-radius: 4px; margin-top: 25px; font-size: 16px; }
                                .footer { font-size: 11px; color: #737373; margin-top: 40px; border-top: 1px solid #222; padding-top: 20px; }
                            </style>
                        </head>
                        <body>
                            <div class="container">
                                <div class="logo">NETFLIX</div>
                                <h2 style="margin-top:0; font-size: 24px; font-weight: 800;">Unlimited entertainment is waiting.</h2>
                                <p style="color: #c0c0c0; font-size: 15px;">Watch anywhere. Cancel anytime. Ready to watch? Complete your subscription today and start enjoying the best movies and TV series offline.</p>
                                <a href="https://netflix.com" class="btn">Finish Sign-Up</a>
                                <div class="footer">
                                    Questions? Visit our Help Center.<br/>Netflix International B.V.
                                </div>
                            </div>
                        </body>
                        </html>
                    """.trimIndent(),
                    receivedAt = accountCreatedTime + 35000L
                )
            }
            messageId.endsWith("_newsletter") -> {
                ProviderMessageDetail(
                    id = messageId,
                    fromName = "Tech Weekly",
                    fromEmail = "newsletter@techweekly.xyz",
                    subject = "Tech Weekly #142: The future of Jetpack Compose and AI Developers",
                    textBody = "Tech Weekly Edition #142\n\nAI coding assistants and modern declarative architectures like Jetpack Compose are reshaping the mobile ecosystem. Developers are launching production-grade native applications at 10x speeds compared to traditional views.\n\nDownload our latest comprehensive PDF report inside to view benchmarks, developer survey results, and multi-platform compilation times.\n\nStay curious!\nTech Weekly Team",
                    htmlBody = """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <style>
                                body { font-family: system-ui, sans-serif; line-height: 1.6; color: #334155; padding: 20px; background-color: #f8fafc; }
                                .box { max-width: 600px; margin: 0 auto; background: #ffffff; border: 1px solid #e2e8f0; border-radius: 12px; padding: 36px; box-shadow: 0 10px 15px -3px rgba(0,0,0,0.05); }
                                .title { border-left: 4px solid #0f172a; padding-left: 16px; margin-bottom: 24px; }
                                .attachment-tag { display: flex; align-items: center; background-color: #f1f5f9; padding: 12px 16px; border-radius: 8px; margin: 20px 0; border: 1.5px solid #cbd5e1; font-weight: bold; font-size: 13px; color: #0f172a; }
                                .footer { font-size: 12px; color: #64748b; text-align: center; margin-top: 40px; border-top: 1px solid #f1f5f9; padding-top: 20px; }
                            </style>
                        </head>
                        <body>
                            <div class="box">
                                <div class="title">
                                    <h4 style="margin:0; color:#64748b; text-transform:uppercase; font-size:12px; letter-spacing:1px;">Technology Newsletter</h4>
                                    <h2 style="margin:5px 0 0; font-weight:800; color:#0f172a;">Tech Weekly #142</h2>
                                </div>
                                <p>In this week's edition, we analyze how <b>Jetpack Compose</b> combined with advanced AI models allows for rapid iteration of user interfaces, responsive design classes, and type-safe navigation.</p>
                                <p>We've attached our fully detailed, 42-page PDF report which includes industry benchmarks, platform support metrics, and optimized styling checklists.</p>
                                <div class="attachment-tag">
                                    📎 Attached File: Jetpack_Compose_AI_Trends_2026.pdf (4.8 MB)
                                </div>
                                <p>Enjoy reading, and stay curious!</p>
                                <div class="footer">
                                    You received this email because you subscribed to Tech Weekly. unsubscribe anytime.<br/>&copy; 2026 Tech Weekly Media.
                                </div>
                            </div>
                        </body>
                        </html>
                    """.trimIndent(),
                    receivedAt = accountCreatedTime + 60000L,
                    attachments = listOf(
                        ProviderAttachment(
                            id = "${messageId}_pdf",
                            filename = "Jetpack_Compose_AI_Trends_2026.pdf",
                            contentType = "application/pdf",
                            sizeBytes = 5033164L,
                            downloadUrl = "https://example.com/compose_ai_trends.pdf"
                        )
                    )
                )
            }
            else -> {
                ProviderMessageDetail(
                    id = messageId,
                    fromName = "System Operator",
                    fromEmail = "operator@simulation.net",
                    subject = "Simulated Message Details",
                    textBody = "This is a simulated secure offline message detail placeholder for id: $messageId.",
                    receivedAt = System.currentTimeMillis()
                )
            }
        }
    }

    override suspend fun deleteMessage(
        accountId: String,
        token: String,
        messageId: String
    ): Boolean = true
}
