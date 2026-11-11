package com.bolobhai.data

/** The profiles shipped on first run (BoloBhai.md §11), copied only if the folder is empty. */
object DefaultProfiles {
    val files: Map<String, String> = mapOf(
        "hinglish-clean.md" to """
            ---
            name: Hinglish Clean
            ---
            I speak Hinglish (Hindi + English). The input may be in Devanagari.
            - Write everything in Roman (English) letters. Never use Devanagari.
              Example: "कल मीटिंग है" -> "kal meeting hai".
            - Do not translate. Keep Hindi words as I said them.
            - I only speak Hindi and English. Never output any other language. If text looks like
              another language, it is a recognition mistake: rewrite it as the Hinglish I most likely said.
            - Fix punctuation and obvious recognition mistakes. Keep names and tech terms exact.
            - If I count items (first/second/third, pehla/dusra/teesra, 1/2/3), write a numbered list,
              one item per line, without the counting words.
            - Follow my spoken corrections.
            - Return only the final text. No explanation.
        """.trimIndent() + "\n",

        "numbered-list.md" to """
            ---
            name: Numbered List
            ---
            Turn what I said into a clean numbered list.
            - One item per line.
            - Remove counting words (pehla, dusra, teesra, first, second).
            - If I said a heading before the items, put it on the first line followed by a colon.
            - Keep Hinglish in Roman letters. Do not translate.
            - Return only the list.
        """.trimIndent() + "\n",

        "email.md" to """
            ---
            name: Email
            ---
            Turn my dictation into a short, polite email.
            - Add a greeting and a sign-off.
            - Keep names, dates and numbers exact.
            - Keep my language mix (Hinglish stays Hinglish in Roman letters) unless I speak only English.
            - Return only the email body.
        """.trimIndent() + "\n",

        "whatsapp-message.md" to """
            ---
            name: WhatsApp Message
            ---
            Write a casual WhatsApp message in Roman Hinglish, the way I said it.
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
