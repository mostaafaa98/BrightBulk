package com.brightbulk.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class MultiChannelActivity : Activity() {

    private lateinit var token: EditText
    private lateinit var chatId: EditText
    private lateinit var message: EditText
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 28, 28, 28)
            setBackgroundColor(Color.rgb(20, 21, 24))
        }

        val title = TextView(this).apply {
            text = "🚀 Bright Bulk — Multi-Channel"
            textSize = 22f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 24)
        }
        root.addView(title)

        val channel = Spinner(this)
        channel.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("Telegram", "WhatsApp", "Messenger", "Instagram")
        )
        root.addView(channel, lp())

        token = field("Telegram Bot Token")
        root.addView(token, lp())

        chatId = field("Telegram Chat ID")
        root.addView(chatId, lp())

        message = field("الرسالة")
        message.minLines = 5
        message.gravity = Gravity.TOP
        root.addView(message, lp())

        val test = Button(this).apply {
            text = "🔎 اختبار الاتصال"
            setOnClickListener { testTelegram() }
        }
        root.addView(test, lp())

        val send = Button(this).apply {
            text = "📨 إرسال Telegram"
            setOnClickListener { sendTelegram() }
        }
        root.addView(send, lp())

        val accessibility = Button(this).apply {
            text = "♿ إعدادات Accessibility"
            setOnClickListener {
                startActivity(android.content.Intent(
                    android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS
                ))
            }
        }
        root.addView(accessibility, lp())

        status = TextView(this).apply {
            text = "جاهز"
            textSize = 15f
            setTextColor(Color.LTGRAY)
            setPadding(0, 24, 0, 0)
        }
        root.addView(status, lp())

        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun field(hint: String) = EditText(this).apply {
        this.hint = hint
        setTextColor(Color.WHITE)
        setHintTextColor(Color.GRAY)
        setBackgroundColor(Color.rgb(38, 40, 45))
        setPadding(20, 16, 20, 16)
    }

    private fun lp() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply {
        setMargins(0, 8, 0, 8)
    }

    private fun apiUrl(method: String): String {
        return "https://api.telegram.org/bot${token.text.toString().trim()}/$method"
    }

    private fun testTelegram() {
        val t = token.text.toString().trim()
        if (t.isEmpty()) {
            status.text = "❌ اكتب Bot Token أولًا"
            return
        }

        status.text = "⏳ جاري اختبار Telegram..."

        Thread {
            try {
                val conn = URL(apiUrl("getMe")).openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                val result = conn.inputStream.bufferedReader().use { it.readText() }

                runOnUiThread {
                    if (result.contains("\"ok\":true")) {
                        status.text = "✅ Telegram Bot متصل"
                    } else {
                        status.text = "❌ Bot Token غير صالح"
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    status.text = "❌ خطأ اتصال: ${e.message}"
                }
            }
        }.start()
    }

    private fun sendTelegram() {
        val t = token.text.toString().trim()
        val id = chatId.text.toString().trim()
        val msg = message.text.toString()

        if (t.isEmpty() || id.isEmpty() || msg.isEmpty()) {
            status.text = "❌ أكمل Token + Chat ID + الرسالة"
            return
        }

        status.text = "⏳ جاري الإرسال..."

        Thread {
            try {
                val data =
                    "chat_id=${URLEncoder.encode(id, "UTF-8")}" +
                    "&text=${URLEncoder.encode(msg, "UTF-8")}"

                val conn = URL(apiUrl("sendMessage")).openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty(
                    "Content-Type",
                    "application/x-www-form-urlencoded"
                )

                conn.outputStream.use {
                    it.write(data.toByteArray(Charsets.UTF_8))
                }

                val response =
                    (if (conn.responseCode in 200..299)
                        conn.inputStream
                    else
                        conn.errorStream)
                        .bufferedReader()
                        .use { it.readText() }

                runOnUiThread {
                    status.text =
                        if (response.contains("\"ok\":true"))
                            "✅ تم إرسال الرسالة"
                        else
                            "❌ Telegram رفض الإرسال"
                }
            } catch (e: Exception) {
                runOnUiThread {
                    status.text = "❌ خطأ: ${e.message}"
                }
            }
        }.start()
    }
}
