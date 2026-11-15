package com.bolobhai.data

/**
 * The profiles shipped on first run (BoloBhai.md §11), copied only if the folder is empty.
 *
 * The universal cleanup rules (language, script, punctuation, return-only-text) live in the locked
 * base system prompt (domain/SystemPrompt.kt); each profile here carries only its own instruction.
 */
object DefaultProfiles {
    val files: Map<String, String> = mapOf(
        "hinglish.md" to """
            ---
            name: Hinglish
            ---
            Transliterate all Hindi/Hinglish words (including Devanagari script) into natural Roman script (Hinglish) as commonly typed in India (e.g., "abhi check karke batao"). Keep English words in standard spelling. Do not translate Hindi into English meaning.
        """.trimIndent() + "\n",

        "proper-english.md" to """
            ---
            name: Proper English
            ---
            Translate all Hindi and Hinglish phrases into clear, grammatically correct, and natural English while preserving the exact original meaning and tone.
        """.trimIndent() + "\n",

        "ai-prompt.md" to """
            ---
            name: AI Prompt
            ---
            Translate and convert the text into a clear, well-structured, and effective prompt in English meant to be pasted into an AI assistant. Organize requirements cleanly.
        """.trimIndent() + "\n",

        "whatsapp-message.md" to """
            ---
            name: WhatsApp Message
            ---
            Translate and style the spoken content (regardless of input language) into a casual, conversational, and chat-friendly WhatsApp message in English.
            Keep it concise, warm, and natural for instant messaging; avoid overly formal or robotic phrasing.
        """.trimIndent() + "\n",

        "professional-email.md" to """
            ---
            name: Professional Email
            ---
            Translate and format the spoken content (regardless of input language) into a polished, polite, and professional email in English.
            Include a concise, relevant "Subject:" line at the top, an appropriate greeting, short readable paragraphs (or bullet points for multiple items), and a standard professional sign-off.
        """.trimIndent() + "\n",
    )
}
