package com.brightbulk.app

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.graphics.Color
import android.view.Gravity
import android.widget.*
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var customers: TextView
    private var imported = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
    }

    private fun tv(text:String, size:Float=16f): TextView = TextView(this).apply {
        this.text=text; textSize=size; setTextColor(Color.rgb(25,25,25))
        setPadding(8,8,8,8)
    }

    private fun btn(text:String)=Button(this).apply { this.text=text }

    private fun buildUi() {
        val scroll=ScrollView(this)
        val box=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            setPadding(28,28,28,28)
        }
        box.addView(tv("BRIGHT BULK",28f))
        box.addView(tv("WhatsApp Campaign Manager",15f))
        status=tv("جاهز — لم يتم استيراد عملاء بعد.")
        box.addView(status)

        val importBtn=btn("📂 استيراد CSV")
        importBtn.setOnClickListener {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type="text/*"; addCategory(Intent.CATEGORY_OPENABLE)
            },100)
        }
        box.addView(importBtn)

        box.addView(tv("الرسالة",19f))
        val msg=EditText(this).apply {
            hint="أهلاً {{name}} 👋\nعندنا كتالوج برايت الجديد."
            minLines=5; gravity=Gravity.TOP
        }
        box.addView(msg)

        box.addView(tv("التأخير بين الرسائل (بالثواني)",17f))
        val delay=EditText(this).apply { hint="5"; inputType=2 }
        box.addView(delay)

        val start=btn("🚀 بدء الحملة")
        start.setOnClickListener {
            if(imported==0) Toast.makeText(this,"استورد العملاء أولاً",Toast.LENGTH_SHORT).show()
            else {
                status.text="الحملة جاهزة للتشغيل — العملاء: $imported"
                Toast.makeText(this,"تم تجهيز الحملة",Toast.LENGTH_SHORT).show()
            }
        }
        box.addView(start)

        val stop=btn("⏹ إيقاف الحملة")
        stop.setOnClickListener { status.text="تم إيقاف الحملة." }
        box.addView(stop)

        customers=tv("العملاء المستوردون: 0",18f)
        box.addView(customers)

        scroll.addView(box); setContentView(scroll)
    }

    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?) {
        super.onActivityResult(requestCode,resultCode,data)
        if(requestCode==100 && resultCode==RESULT_OK) {
            val uri=data?.data ?: return
            try {
                val input=contentResolver.openInputStream(uri) ?: return
                val lines=BufferedReader(InputStreamReader(input)).readLines()
                imported=(lines.size-1).coerceAtLeast(0)
                customers.text="العملاء المستوردون: $imported"
                status.text="تم استيراد الملف بنجاح."
            } catch(e:Exception) {
                status.text="تعذر قراءة الملف: ${e.message}"
            }
        }
    }
}
