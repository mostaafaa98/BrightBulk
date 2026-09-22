package com.brightbulk.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

private data class V2Contact(
    val id: String,
    var name: String,
    var phone: String,
    var tags: String = "",
    var telegramId: String = "",
    var whatsappId: String = ""
)

private data class V2Template(
    var id: String,
    var name: String,
    var channel: String,
    var body: String
)

class BrightBulkV2Activity : Activity() {
    private val prefs by lazy { getSharedPreferences("bright_bulk_v2", MODE_PRIVATE) }
    private val contacts = mutableListOf<V2Contact>()
    private val templates = mutableListOf<V2Template>()
    private val logs = mutableListOf<String>()
    private var root: LinearLayout? = null
    private var content: LinearLayout? = null
    private var page = "الرئيسية"

    private val bg = Color.rgb(11, 12, 14)
    private val panel = Color.rgb(24, 26, 30)
    private val panel2 = Color.rgb(31, 34, 39)
    private val text = Color.rgb(242, 244, 247)
    private val muted = Color.rgb(158, 164, 174)
    private val stroke = Color.rgb(65, 70, 79)

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        loadLocal()
        buildShell()
        dashboard()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun label(value: String, size: Float = 14f, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(text)
            if (bold) typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(4), dp(5), dp(4), dp(5))
        }

    private fun button(value: String, primary: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = 13.5f
            gravity = Gravity.CENTER
            setTextColor(text)
            if (primary) typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(if (primary) Color.rgb(52, 58, 68) else panel2, 16, stroke)
            isClickable = true
            minHeight = dp(48)
        }

    private fun field(hint: String, value: String = "", multi: Boolean = false) =
        EditText(this).apply {
            this.hint = hint
            setText(value)
            textSize = 14f
            setTextColor(text)
            setHintTextColor(muted)
            background = rounded(panel2, 15, stroke)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            inputType = if (multi) {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            } else {
                InputType.TYPE_CLASS_TEXT
            }
            if (multi) {
                minLines = 4
                gravity = Gravity.TOP or Gravity.RIGHT
            }
        }

    private fun rounded(fill: Int, radius: Int, border: Int = Color.TRANSPARENT) =
        android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = dp(radius).toFloat()
            setColor(fill)
            if (border != Color.TRANSPARENT) setStroke(dp(1), border)
        }

    private fun buildShell() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(12))
            setBackgroundColor(Color.rgb(18, 20, 23))
        }

        val logo = ImageView(this).apply {
            setImageResource(R.drawable.splash_logo)
            background = rounded(panel2, 16, stroke)
            setPadding(dp(5), dp(5), dp(5), dp(5))
        }
        header.addView(logo, LinearLayout.LayoutParams(dp(48), dp(48)))

        val brand = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }
        brand.addView(label("BRIGHT BULK", 18f, true))
        brand.addView(label("2.0 • Omnichannel Campaigns", 11f).apply { setTextColor(muted) })
        header.addView(brand, LinearLayout.LayoutParams(0, -2, 1f))

        val status = label("● V2", 11f, true)
        status.setTextColor(Color.rgb(220, 225, 232))
        status.background = rounded(panel2, 18, stroke)
        status.setPadding(dp(10), dp(7), dp(10), dp(7))
        header.addView(status)

        root?.addView(header, LinearLayout.LayoutParams(-1, dp(76)))

        val scroll = ScrollView(this).apply { isFillViewport = true }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(24))
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        scroll.addView(content)
        root?.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(6), dp(6), dp(6), dp(8))
            setBackgroundColor(Color.rgb(18, 20, 23))
        }
        val items = listOf(
            "الرئيسية" to "⌂",
            "العملاء" to "♙",
            "الحملات" to "✦",
            "القنوات" to "◎",
            "المزيد" to "☰"
        )
        for ((name, icon) in items) {
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(6), dp(2), dp(6), dp(2))
                background = rounded(if (page == name) panel2 else Color.TRANSPARENT, 16)
                setOnClickListener {
                    when (name) {
                        "الرئيسية" -> dashboard()
                        "العملاء" -> contactsPage()
                        "الحملات" -> campaignsPage()
                        "القنوات" -> channelsPage()
                        "المزيد" -> morePage()
                    }
                }
            }
            item.addView(label(icon, 19f, true).apply { gravity = Gravity.CENTER })
            item.addView(label(name, 10f).apply { gravity = Gravity.CENTER; setTextColor(muted) })
            nav.addView(item, LinearLayout.LayoutParams(0, dp(62), 1f))
        }
        root?.addView(nav, LinearLayout.LayoutParams(-1, dp(74)))
        setContentView(root)
    }

    private fun refresh() {
        buildShell()
        when (page) {
            "العملاء" -> contactsPage()
            "الحملات" -> campaignsPage()
            "القنوات" -> channelsPage()
            "المزيد" -> morePage()
            else -> dashboard()
        }
    }

    private fun clearPage(title: String, subtitle: String = "") {
        content?.removeAllViews()
        content?.addView(label(title, 25f, true))
        if (subtitle.isNotBlank()) content?.addView(label(subtitle, 13f).apply { setTextColor(muted) })
    }

    private fun section(title: String) {
        content?.addView(label(title, 15f, true).apply {
            setPadding(dp(4), dp(18), dp(4), dp(8))
        })
    }

    private fun metric(title: String, value: String) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(13), dp(16), dp(13))
            background = rounded(panel, 18, stroke)
        }
        box.addView(label(title, 12f).apply { setTextColor(muted) })
        box.addView(label(value, 24f, true))
        val lp = LinearLayout.LayoutParams(0, dp(88), 1f)
        lp.setMargins(dp(4), 0, dp(4), dp(8))
        content?.addView(box, lp)
    }

    private fun dashboard() {
        page = "الرئيسية"
        clearPage("لوحة التحكم", "إدارة العملاء والحملات والقنوات من مكان واحد.")

        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        metric("العملاء", contacts.size.toString())
        // metric() appends directly, so use a compact dedicated row instead.
        content?.removeViewAt(content!!.childCount - 1)
        content?.removeViewAt(content!!.childCount - 1)

        val cards = listOf(
            "العملاء" to contacts.size.toString(),
            "القوالب" to templates.size.toString(),
            "القنوات" to "4",
            "النظام" to "V2"
        )
        for (i in 0 until cards.size step 2) {
            val r = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            for (j in i until minOf(i + 2, cards.size)) {
                val (t, v) = cards[j]
                val b = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(14), dp(12), dp(14), dp(12))
                    background = rounded(panel, 18, stroke)
                }
                b.addView(label(t, 12f).apply { setTextColor(muted) })
                b.addView(label(v, 23f, true))
                r.addView(b, LinearLayout.LayoutParams(0, dp(88), 1f).apply {
                    setMargins(dp(4), 0, dp(4), dp(8))
                })
            }
            content?.addView(r)
        }

        section("ابدأ بسرعة")
        val add = button("＋ إضافة عميل")
        add.setOnClickListener { addContactDialog() }
        content?.addView(add, lp())

        val import = button("⇩ استيراد CSV / TXT")
        import.setOnClickListener { chooseFile() }
        content?.addView(import, lp())

        val campaign = button("✦ إنشاء حملة جديدة", true)
        campaign.setOnClickListener { campaignsPage() }
        content?.addView(campaign, lp())

        val test = button("◉ اختبار اتصال الـ Backend")
        test.setOnClickListener { testBackend() }
        content?.addView(test, lp())

        section("الوضع الحالي")
        val note = label(
            "المحرك الجديد يفصل واجهة التطبيق عن الإرسال: Android يدير البيانات والواجهة، والـ Backend يدير الطوابير والقنوات والتسجيلات.",
            13f
        )
        note.setTextColor(muted)
        content?.addView(note)
    }

    private fun contactsPage() {
        page = "العملاء"
        clearPage("العملاء", "ملف موحّد للعميل مع معرفات القنوات المختلفة.")

        val add = button("＋ عميل جديد", true)
        add.setOnClickListener { addContactDialog() }
        content?.addView(add, lp())

        val import = button("⇩ استيراد CSV / TXT")
        import.setOnClickListener { chooseFile() }
        content?.addView(import, lp())

        section("قائمة العملاء")
        if (contacts.isEmpty()) {
            content?.addView(label("لا يوجد عملاء بعد. استورد ملفك أو أضف أول عميل.", 13f).apply { setTextColor(muted) })
            return
        }

        for (c in contacts.take(300)) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(10), dp(14), dp(10))
                background = rounded(panel, 16, stroke)
            }
            row.addView(label(c.name, 15f, true))
            row.addView(label(c.phone.ifBlank { "بدون هاتف" }, 12f).apply { setTextColor(muted) })
            val channels = listOf(
                if (c.whatsappId.isNotBlank()) "WA" else "",
                if (c.telegramId.isNotBlank()) "TG" else ""
            ).filter { it.isNotBlank() }.joinToString(" • ")
            row.addView(label(if (channels.isBlank()) "بدون معرف قناة" else channels, 11f).apply { setTextColor(muted) })
            val lp = lp()
            lp.setMargins(0, 0, 0, dp(8))
            content?.addView(row, lp)
        }
    }

    private fun campaignsPage() {
        page = "الحملات"
        clearPage("Campaign Builder", "حملة واحدة + جمهور + قناة + رسالة + جدولة + سجل نتائج.")

        val name = field("اسم الحملة")
        val channel = Spinner(this).apply {
            adapter = ArrayAdapter(this@BrightBulkV2Activity, android.R.layout.simple_spinner_dropdown_item,
                arrayOf("whatsapp", "telegram", "instagram", "messenger"))
        }
        val message = field("نص الرسالة — استخدم {{name}} للتخصيص", multi = true)

        val delay = Spinner(this).apply {
            adapter = ArrayAdapter(this@BrightBulkV2Activity, android.R.layout.simple_spinner_dropdown_item,
                arrayOf("1", "2", "5", "10", "15", "30").map { "$it ثانية" })
        }

        content?.addView(name, lp())
        content?.addView(label("القناة", 12f).apply { setTextColor(muted) })
        content?.addView(channel, lp())
        content?.addView(label("الرسالة", 12f).apply { setTextColor(muted) })
        content?.addView(message, lp())
        content?.addView(label("الفاصل بين الرسائل", 12f).apply { setTextColor(muted) })
        content?.addView(delay, lp())

        val selectedIds = contacts.map { it.id }
        val audience = label("الجمهور الحالي: ${selectedIds.size} عميل", 13f)
        audience.setTextColor(muted)
        content?.addView(audience)

        val create = button("حفظ الحملة", true)
        create.setOnClickListener {
            if (name.text.toString().trim().isBlank() || message.text.toString().trim().isBlank()) {
                toast("اكتب اسم الحملة والرسالة أولاً")
                return@setOnClickListener
            }
            createCampaign(
                name.text.toString().trim(),
                channel.selectedItem.toString(),
                message.text.toString(),
                (delay.selectedItem.toString().substringBefore(" ")).toLong() * 1000L,
                selectedIds
            )
        }
        content?.addView(create, lp())

        section("الحملات المحفوظة")
        fetchAndRenderCampaigns()
    }

    private fun channelsPage() {
        page = "القنوات"
        clearPage("القنوات", "القناة لا تُرسل من داخل Activity؛ كل قناة لها Adapter رسمي في الـ Backend.")

        val backend = field("Backend URL", prefs.getString("backend_url", "http://127.0.0.1:8787") ?: "")
        content?.addView(backend, lp())

        val save = button("حفظ عنوان الـ Backend", true)
        save.setOnClickListener {
            prefs.edit().putString("backend_url", backend.text.toString().trim().removeSuffix("/")).apply()
            toast("تم حفظ العنوان")
        }
        content?.addView(save, lp())

        section("WhatsApp Business")
        content?.addView(label(
            "Cloud API • يحتاج Meta Business Portfolio + WABA + رقم أعمال + Access Token. لا تضع التوكن داخل الكود.",
            13f
        ).apply { setTextColor(muted) })
        content?.addView(label("الحالة: تُقرأ من إعدادات السيرفر.", 12f).apply { setTextColor(muted) })

        section("Telegram")
        content?.addView(label(
            "Bot API • استهدف chat_id صحيح. رقم الهاتف وحده ليس chat_id.",
            13f
        ).apply { setTextColor(muted) })

        section("Instagram / Messenger")
        content?.addView(label(
            "هيكل القنوات موجود، والإرسال يُضاف عبر Meta APIs الرسمية عند ربط الحسابات والصلاحيات المناسبة.",
            13f
        ).apply { setTextColor(muted) })

        val test = button("اختبار Backend")
        test.setOnClickListener { testBackend() }
        content?.addView(test, lp())
    }

    private fun morePage() {
        page = "المزيد"
        clearPage("المزيد", "القوالب والأتمتة والسجلات والإعدادات.")

        val templatesButton = button("▤ القوالب")
        templatesButton.setOnClickListener { templatesPage() }
        content?.addView(templatesButton, lp())

        val automations = button("⚡ Automations")
        automations.setOnClickListener { automationsPage() }
        content?.addView(automations, lp())

        val logs = button("☷ Logs")
        logs.setOnClickListener { logsPage() }
        content?.addView(logs, lp())

        val legacy = button("فتح الوضع القديم")
        legacy.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
        content?.addView(legacy, lp())

        section("ملاحظة")
        content?.addView(label(
            "الوضع القديم محفوظ للتراجع والاختبار. المحرك الجديد لا يستخدم Accessibility للضغط على زر Send في WhatsApp.",
            13f
        ).apply { setTextColor(muted) })
    }

    private fun templatesPage() {
        page = "المزيد"
        clearPage("القوالب", "قوالب قابلة لإعادة الاستخدام حسب القناة.")

        val name = field("اسم القالب")
        val channel = field("القناة: whatsapp / telegram")
        val body = field("نص القالب", multi = true)

        content?.addView(name, lp())
        content?.addView(channel, lp())
        content?.addView(body, lp())

        val save = button("حفظ القالب", true)
        save.setOnClickListener {
            val n = name.text.toString().trim()
            val ch = channel.text.toString().trim().lowercase(Locale.ROOT).ifBlank { "whatsapp" }
            val b = body.text.toString()
            if (n.isBlank() || b.isBlank()) return@setOnClickListener
            templates.add(V2Template("local_${System.currentTimeMillis()}", n, ch, b))
            persistTemplates()
            toast("تم حفظ القالب")
            templatesPage()
        }
        content?.addView(save, lp())

        section("القوالب الحالية")
        for (t in templates) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(13), dp(10), dp(13), dp(10))
                background = rounded(panel, 16, stroke)
            }
            row.addView(label(t.name, 14f, true))
            row.addView(label("${t.channel} • ${t.body.take(120)}", 12f).apply { setTextColor(muted) })
            val lp = lp(); lp.setMargins(0, 0, 0, dp(7))
            content?.addView(row, lp)
        }
    }

    private fun automationsPage() {
        page = "المزيد"
        clearPage("Automations", "أساس Rules / Triggers / Actions للمرحلة التالية.")

        val trigger = field("Trigger — مثال: inbound_reply")
        val action = field("Action — مثال: stop_campaign")
        val save = button("حفظ Rule", true)
        save.setOnClickListener {
            prefs.edit()
                .putString("automation_trigger", trigger.text.toString())
                .putString("automation_action", action.text.toString())
                .apply()
            toast("تم حفظ الـ Rule")
        }
        content?.addView(trigger, lp())
        content?.addView(action, lp())
        content?.addView(save, lp())

        section("Rule الحالية")
        val t = prefs.getString("automation_trigger", "") ?: ""
        val a = prefs.getString("automation_action", "") ?: ""
        content?.addView(label(
            if (t.isBlank()) "لا توجد Rule محفوظة." else "$t  →  $a",
            13f
        ).apply { setTextColor(muted) })
    }

    private fun logsPage() {
        page = "المزيد"
        clearPage("Logs", "سجل التشغيل والنتائج والأخطاء.")

        if (logs.isEmpty()) {
            fetchLogs()
            content?.addView(label("جارٍ تحميل السجل…", 13f).apply { setTextColor(muted) })
            return
        }
        for (line in logs.take(200)) {
            val r = label(line, 12f)
            r.setTextColor(muted)
            content?.addView(r)
        }
    }

    private fun addContactDialog() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
        }
        val name = field("الاسم")
        val phone = field("رقم الهاتف")
        val tg = field("Telegram chat_id — اختياري")
        val wa = field("WhatsApp wa_id — اختياري")
        box.addView(name, lp())
        box.addView(phone, lp())
        box.addView(tg, lp())
        box.addView(wa, lp())

        AlertDialog.Builder(this)
            .setTitle("إضافة عميل")
            .setView(box)
            .setNegativeButton("إلغاء", null)
            .setPositiveButton("حفظ") { _, _ ->
                val c = V2Contact(
                    "local_${System.currentTimeMillis()}",
                    name.text.toString().trim().ifBlank { "عميل" },
                    normalizePhone(phone.text.toString()),
                    "",
                    tg.text.toString().trim(),
                    wa.text.toString().trim()
                )
                contacts.add(c)
                persistContacts()
                syncContact(c)
                refresh()
            }
            .show()
    }

    private fun chooseFile() {
        startActivityForResult(
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            },
            100
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 100 || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        thread {
            try {
                val raw = contentResolver.openInputStream(uri)?.use {
                    BufferedReader(InputStreamReader(it, StandardCharsets.UTF_8)).readText()
                } ?: ""
                val imported = parseCsv(raw)
                runOnUiThread {
                    contacts.clear()
                    contacts.addAll(imported)
                    persistContacts()
                    syncContacts()
                    toast("تم استيراد ${imported.size} عميل")
                    contactsPage()
                }
            } catch (e: Exception) {
                runOnUiThread { toast("خطأ في الاستيراد: ${e.message}") }
            }
        }
    }

    private fun parseCsv(raw: String): List<V2Contact> {
        val rows = raw.lines().map { it.removePrefix("\uFEFF") }.filter { it.isNotBlank() }
        if (rows.isEmpty()) return emptyList()

        val delimiter = when {
            rows.first().contains('\t') -> '\t'
            rows.first().count { it == ';' } > rows.first().count { it == ',' } -> ';'
            else -> ','
        }

        val header = split(rows.first(), delimiter).map { it.trim().lowercase(Locale.ROOT) }
        val phoneHeaders = setOf("phone","mobile","telephone","tel","number","phone number","mobile number","whatsapp","رقم","رقم الهاتف","الموبايل","المحمول","واتساب")
        val nameHeaders = setOf("name","full name","customer","customer name","الاسم","اسم","اسم العميل","العميل")
        val tgHeaders = setOf("telegram","telegram chat id","telegram_chat_id","chat_id")
        val waHeaders = setOf("whatsapp wa id","whatsapp_wa_id","wa_id","whatsapp")

        val pIndex = header.indexOfFirst { phoneHeaders.contains(it) }
        val nIndex = header.indexOfFirst { nameHeaders.contains(it) }
        val tIndex = header.indexOfFirst { tgHeaders.contains(it) }
        val wIndex = header.indexOfFirst { waHeaders.contains(it) }
        val hasHeader = listOf(pIndex, nIndex, tIndex, wIndex).any { it >= 0 }
        val start = if (hasHeader) 1 else 0

        val out = mutableListOf<V2Contact>()
        for (line in rows.drop(start)) {
            val cells = split(line, delimiter).map { it.trim() }
            if (cells.isEmpty()) continue
            val pi = if (pIndex in cells.indices) pIndex else cells.indexOfFirst { normalizePhone(it).length >= 10 }
            if (pi !in cells.indices) continue

            val phone = normalizePhone(cells[pi])
            if (phone.length < 10) continue

            val name = if (nIndex in cells.indices) cells[nIndex].ifBlank { "عميل" } else "عميل"
            val tg = if (tIndex in cells.indices) cells[tIndex] else ""
            val wa = if (wIndex in cells.indices) cells[wIndex] else ""
            out.add(V2Contact("local_${out.size}_${System.currentTimeMillis()}", name, phone, "", tg, wa))
        }
        return out.distinctBy { "${it.name}|${it.phone}|${it.telegramId}|${it.whatsappId}" }
    }

    private fun split(line: String, delimiter: Char): List<String> {
        val out = mutableListOf<String>()
        val b = StringBuilder()
        var quoted = false
        for (ch in line) {
            when {
                ch == '"' -> quoted = !quoted
                ch == delimiter && !quoted -> {
                    out.add(b.toString())
                    b.setLength(0)
                }
                else -> b.append(ch)
            }
        }
        out.add(b.toString())
        return out
    }

    private fun createCampaign(
        name: String,
        channel: String,
        message: String,
        delayMs: Long,
        contactIds: List<String>
    ) {
        val obj = JSONObject().apply {
            put("name", name)
            put("channel", channel)
            put("body", message)
            put("delayMs", delayMs)
            put("contactIds", JSONArray(contactIds))
        }
        apiPost("/api/campaigns", obj) { ok, response ->
            runOnUiThread {
                if (ok) {
                    toast("تم إنشاء الحملة")
                    campaignsPage()
                } else {
                    toast("تعذر إنشاء الحملة: $response")
                }
            }
        }
    }

    private fun fetchAndRenderCampaigns() {
        apiGet("/api/campaigns") { ok, response ->
            runOnUiThread {
                if (!ok) {
                    content?.addView(label("Backend غير متصل أو لم يبدأ بعد.", 13f).apply { setTextColor(muted) })
                    return@runOnUiThread
                }
                val arr = JSONObject(response).optJSONArray("campaigns") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val c = arr.getJSONObject(i)
                    val row = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(dp(14), dp(10), dp(14), dp(10))
                        background = rounded(panel, 16, stroke)
                    }
                    row.addView(label(c.optString("name"), 14f, true))
                    row.addView(label(
                        "${c.optString("channel")} • ${c.optString("status")} • sent ${c.optInt("sent")}/${c.optInt("queued") + c.optInt("sent") + c.optInt("failed")}",
                        12f
                    ).apply { setTextColor(muted) })
                    val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
                    val start = button("▶ Start", true)
                    start.setOnClickListener { campaignAction(c.optString("id"), "start") }
                    val pause = button("Ⅱ Pause")
                    pause.setOnClickListener { campaignAction(c.optString("id"), "pause") }
                    val stop = button("■ Stop")
                    stop.setOnClickListener { campaignAction(c.optString("id"), "stop") }
                    actions.addView(start, LinearLayout.LayoutParams(0, dp(48), 1f))
                    actions.addView(pause, LinearLayout.LayoutParams(0, dp(48), 1f))
                    actions.addView(stop, LinearLayout.LayoutParams(0, dp(48), 1f))
                    row.addView(actions)
                    val lp = lp(); lp.setMargins(0, 0, 0, dp(8))
                    content?.addView(row, lp)
                }
            }
        }
    }

    private fun campaignAction(id: String, action: String) {
        apiPost("/api/campaigns/$id/$action", JSONObject()) { ok, _ ->
            runOnUiThread {
                toast(if (ok) "تم $action" else "فشل تنفيذ $action")
                campaignsPage()
            }
        }
    }

    private fun testBackend() {
        apiGet("/api/health") { ok, response ->
            runOnUiThread {
                toast(if (ok) "✅ Backend متصل" else "❌ Backend غير متصل")
                if (!ok) {
                    AlertDialog.Builder(this)
                        .setTitle("Backend")
                        .setMessage("ابدأ السيرفر من Termux:\n\ncd ~/BrightBulk/backend\nnode server.js")
                        .setPositiveButton("تمام", null)
                        .show()
                }
            }
        }
    }

    private fun syncContacts() {
        val arr = JSONArray()
        contacts.forEach {
            arr.put(JSONObject().apply {
                put("id", it.id)
                put("name", it.name)
                put("phone", it.phone)
                put("tags", JSONArray(it.tags.split(",").map(String::trim).filter(String::isNotBlank)))
                put("channels", JSONObject().apply {
                    put("telegram", it.telegramId)
                    put("whatsapp", it.whatsappId)
                })
            })
        }
        apiPost("/api/contacts/bulk", JSONObject().put("contacts", arr)) { _, _ -> }
    }

    private fun syncContact(c: V2Contact) {
        val body = JSONObject().apply {
            put("id", c.id)
            put("name", c.name)
            put("phone", c.phone)
            put("tags", JSONArray())
            put("channels", JSONObject().apply {
                put("telegram", c.telegramId)
                put("whatsapp", c.whatsappId)
            })
        }
        apiPost("/api/contacts", body) { _, _ -> }
    }

    private fun fetchLogs() {
        apiGet("/api/logs?limit=200") { ok, response ->
            if (!ok) return@apiGet
            val arr = JSONObject(response).optJSONArray("logs") ?: JSONArray()
            logs.clear()
            for (i in 0 until arr.length()) {
                val x = arr.getJSONObject(i)
                logs.add("${x.optString("createdAt")} • ${x.optString("event")} • ${x.optString("detail")}")
            }
            runOnUiThread { logsPage() }
        }
    }

    private fun loadLocal() {
        val contactsJson = prefs.getString("contacts", "") ?: ""
        if (contactsJson.isNotBlank()) {
            val arr = JSONArray(contactsJson)
            for (i in 0 until arr.length()) {
                val x = arr.getJSONObject(i)
                contacts.add(
                    V2Contact(
                        x.optString("id"),
                        x.optString("name"),
                        x.optString("phone"),
                        x.optString("tags"),
                        x.optString("telegramId"),
                        x.optString("whatsappId")
                    )
                )
            }
        }

        val templatesJson = prefs.getString("templates", "") ?: ""
        if (templatesJson.isNotBlank()) {
            val arr = JSONArray(templatesJson)
            for (i in 0 until arr.length()) {
                val x = arr.getJSONObject(i)
                templates.add(V2Template(x.optString("id"), x.optString("name"), x.optString("channel"), x.optString("body")))
            }
        }
    }

    private fun persistContacts() {
        val arr = JSONArray()
        contacts.forEach {
            arr.put(JSONObject().apply {
                put("id", it.id)
                put("name", it.name)
                put("phone", it.phone)
                put("tags", it.tags)
                put("telegramId", it.telegramId)
                put("whatsappId", it.whatsappId)
            })
        }
        prefs.edit().putString("contacts", arr.toString()).apply()
    }

    private fun persistTemplates() {
        val arr = JSONArray()
        templates.forEach {
            arr.put(JSONObject().apply {
                put("id", it.id)
                put("name", it.name)
                put("channel", it.channel)
                put("body", it.body)
            })
        }
        prefs.edit().putString("templates", arr.toString()).apply()
    }

    private fun backendUrl() =
        (prefs.getString("backend_url", "http://127.0.0.1:8787") ?: "http://127.0.0.1:8787").trim().removeSuffix("/")

    private fun apiGet(path: String, callback: (Boolean, String) -> Unit) {
        thread {
            try {
                val c = URL(backendUrl() + path).openConnection() as HttpURLConnection
                c.requestMethod = "GET"
                c.connectTimeout = 12_000
                c.readTimeout = 12_000
                val code = c.responseCode
                val stream = if (code in 200..299) c.inputStream else c.errorStream
                val body = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() } ?: ""
                c.disconnect()
                callback(code in 200..299, body)
            } catch (e: Exception) {
                callback(false, e.message ?: "network error")
            }
        }
    }

    private fun apiPost(path: String, json: JSONObject, callback: (Boolean, String) -> Unit) {
        thread {
            try {
                val c = URL(backendUrl() + path).openConnection() as HttpURLConnection
                c.requestMethod = "POST"
                c.doOutput = true
                c.connectTimeout = 12_000
                c.readTimeout = 12_000
                c.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                c.outputStream.use { it.write(json.toString().toByteArray(StandardCharsets.UTF_8)) }
                val code = c.responseCode
                val stream = if (code in 200..299) c.inputStream else c.errorStream
                val body = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() } ?: ""
                c.disconnect()
                callback(code in 200..299, body)
            } catch (e: Exception) {
                callback(false, e.message ?: "network error")
            }
        }
    }

    private fun normalizePhone(raw: String): String {
        var s = raw.trim().replace(" ", "").replace("-", "").replace("(", "").replace(")", "")
        if (s.startsWith("+")) s = s.drop(1)
        if (s.startsWith("00")) s = s.drop(2)
        if (s.startsWith("01") && s.length == 11) s = "20" + s.drop(1)
        return s.filter { it.isDigit() }
    }

    private fun lp() = LinearLayout.LayoutParams(-1, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
        setMargins(0, 0, 0, dp(8))
    }

    private fun toast(value: String) =
        Toast.makeText(this, value, Toast.LENGTH_SHORT).show()

    override fun onDestroy() {
        super.onDestroy()
    }
}
