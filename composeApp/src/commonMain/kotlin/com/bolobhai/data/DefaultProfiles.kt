package com.bolobhai.data

/**
 * The profiles shipped on first run (BoloBhai.md §11), copied only if the folder is empty.
 *
 * The universal cleanup rules (language, script, punctuation, return-only-text) live in the locked
 * base system prompt (domain/SystemPrompt.kt); each profile here carries only its own instruction.
 */
object DefaultProfiles {
    val files: Map<String, String> = mapOf(
        "hinglish-clean.md" to """
            ---
            name: Hinglish Clean
            ---
            Return the cleaned-up transcript as ordinary prose, the way I said it.
            Do not add structure, headings or lists.
        """.trimIndent() + "\n",

        "numbered-list.md" to """
            ---
            name: Numbered List
            ---
            Turn what I said into a clean numbered list.
            - One item per line.
            - Remove counting words (pehla, dusra, teesra, first, second).
            - If I said a heading before the items, put it on the first line followed by a colon.
            - Return only the list.
        """.trimIndent() + "\n",

        "email.md" to """
            ---
            name: Email
            ---
            Turn my dictation into a short, polite email.
            - Add a greeting and a sign-off.
            - Return only the email body.
        """.trimIndent() + "\n",

        "whatsapp-message.md" to """
            ---
            name: WhatsApp Message
            ---
            Write a casual WhatsApp message, the way I said it.
            - Short sentences. No formal greeting.
            - Return only the message.
        """.trimIndent() + "\n",

        "meeting-notes.md" to """
            ---
            name: Meeting Notes
            ---
            Turn what I said into short meeting notes.
            - One bullet per point, starting with "- ".
            - Keep owners and dates exact.
            - Return only the notes, with "Notes:" as the first line.
        """.trimIndent() + "\n",
    )
}
