package com.brightbulk.app

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.animation.AccelerateDecelerateInterpolator
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.widget.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.Locale
import kotlin.concurrent.thread

data class Customer(val name: String, val phone: String)

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("bright_bulk", MODE_PRIVATE) }
    private val customersList = mutableListOf<Customer>()
    private var running = false
    private var sent = 0
    private var failed = 0
    private var skipped = 0
    private var currentCampaignIndex = 0
    private var campaignMessage = ""
    private var campaignChannel = "WhatsApp Business"
    private var attachmentUri: Uri? = null
    private var attachmentName = ""
    private var delaySeconds = 10
    private val handler = Handler(Looper.getMainLooper())
    private var countdownRunnable: Runnable? = null
    private var nextButton: TextView? = null
    private lateinit var root: LinearLayout
    private lateinit var status: TextView
    private lateinit var content: LinearLayout
    private lateinit var stats: TextView

    private fun dp(v:Int)= (v*resources.displayMetrics.density).toInt()
    private fun rounded(fill:Int,radius:Int,stroke:Int=Color.TRANSPARENT)=GradientDrawable().apply{shape=GradientDrawable.RECTANGLE;cornerRadius=dp(radius).toFloat();setColor(fill);if(stroke!=Color.TRANSPARENT)setStroke(dp(1),stroke)}
    private fun gradient(start:Int,end:Int,radius:Int)=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(start,end)).apply{cornerRadius=dp(radius).toFloat()}
    private fun tv(t:String,s:Float=15f,b:Boolean=false)=TextView(this).apply{text=t;textSize=s;setTextColor(Color.rgb(235,237,240));if(b)typeface=Typeface.DEFAULT_BOLD;setPadding(dp(3),dp(5),dp(3),dp(5))}
    private fun btn(t:String)=TextView(this).apply{text=t;textSize=14f;gravity=Gravity.CENTER;setTextColor(Color.rgb(245,246,248));typeface=Typeface.DEFAULT_BOLD;background=rounded(Color.rgb(38,41,45),17,Color.rgb(82,87,94));setPadding(dp(12),dp(10),dp(12),dp(10));isClickable=true;minHeight=dp(48);elevation=dp(1).toFloat()}
    private fun field(h:String,v:String="",lines:Int=1)=EditText(this).apply{hint=h;setText(v);setTextColor(Color.rgb(238,240,243));setHintTextColor(Color.rgb(145,150,158));background=rounded(Color.rgb(27,29,32),16,Color.rgb(70,74,80));setPadding(dp(14),dp(10),dp(14),dp(10));if(lines>1){minLines=lines;gravity=Gravity.TOP}}

    override fun onCreate(b:Bundle?){super.onCreate(b);loadCustomers();showApp()}
    private fun showApp(){
        root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(Color.rgb(12,13,15));layoutDirection=View.LAYOUT_DIRECTION_RTL}
        val head=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(18),dp(14),dp(18),dp(12));setBackgroundColor(Color.rgb(20,21,24));elevation=dp(5).toFloat()}
        val logo=TextView(this).apply{text="🧶";textSize=25f;gravity=Gravity.CENTER;setTextColor(Color.WHITE);background=rounded(Color.rgb(38,41,45),18,Color.rgb(82,87,94));elevation=dp(4).toFloat()}
        head.addView(logo,LinearLayout.LayoutParams(dp(50),dp(50)))
        val brand=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(12),0,0,0)}
        brand.addView(tv("BRIGHT BULK",19f,true));brand.addView(tv("PRO • Campaign Manager",12f))
        head.addView(brand,LinearLayout.LayoutParams(0,-2,1f))
        status=tv("● جاهز",12f,true);status.setTextColor(Color.rgb(225,228,232));status.background=rounded(Color.rgb(38,41,45),18,Color.rgb(82,87,94));status.setPadding(dp(12),dp(7),dp(12),dp(7));head.addView(status)
        root.addView(head)
        logo.animate().scaleX(1.06f).scaleY(1.06f).setDuration(650).setInterpolator(AccelerateDecelerateInterpolator()).withEndAction{logo.animate().scaleX(1f).scaleY(1f).setDuration(650).start()}.start()
        val scroll=ScrollView(this).apply{isFillViewport=true}
        content=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(16),dp(16),dp(24));layoutDirection=View.LAYOUT_DIRECTION_RTL}
        scroll.addView(content)
        root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        nav=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER;setPadding(dp(8),dp(8),dp(8),dp(9));background=rounded(Color.rgb(20,21,24),24,Color.rgb(55,58,63));elevation=dp(12).toFloat();layoutDirection=View.LAYOUT_DIRECTION_RTL}
        root.addView(nav,LinearLayout.LayoutParams(-1,dp(76)))
        setContentView(root)
        dashboard()
    }
    private lateinit var nav: LinearLayout
    private fun buildNav(active:String){
        nav.removeAllViews()
        listOf("الرئيسية" to "⌂","العملاء" to "♙","الحملة" to "✦","الإعدادات" to "⚙").forEach{(label,icon)->
            val selected=label==active
            val x=LinearLayout(this).apply{
                orientation=LinearLayout.VERTICAL
                gravity=Gravity.CENTER
                background=rounded(if(selected) Color.rgb(38,41,45) else Color.TRANSPARENT,18)
                setPadding(dp(6),dp(3),dp(6),dp(3))
                setOnClickListener{when(label){"الرئيسية"->dashboard();"العملاء"->customers();"الحملة"->campaign();"الإعدادات"->settings()}}
            }
            x.addView(tv(icon,19f,true).apply{setTextColor(if(selected) Color.rgb(245,246,248) else Color.rgb(145,150,158));setGravity(Gravity.CENTER)})
            x.addView(tv(label,10f,selected).apply{setTextColor(if(selected) Color.rgb(245,246,248) else Color.rgb(145,150,158));setGravity(Gravity.CENTER)})
            nav.addView(x,LinearLayout.LayoutParams(0,-1,1f))
        }
    }
    private fun clear(){content.removeAllViews();content.scrollTo(0,0)}
    private fun card(t:String,v:String){
        val x=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(12),dp(16),dp(12));setBackground(rounded(Color.rgb(27,29,32),20,Color.rgb(55,58,63)));elevation=dp(2).toFloat()}
        x.addView(tv(t,13f))
        x.addView(tv(v,24f,true))
        val p=LinearLayout.LayoutParams(-1,dp(92))
        p.setMargins(0,0,0,dp(10))
        content.addView(x,p)
        x.alpha=0f
        x.translationY=dp(10).toFloat()
        x.animate().alpha(1f).translationY(0f).setDuration(260).start()
    }

    private fun statValue():Array<Int>{
        val seen=mutableSetOf<String>()
        var valid=0
        var invalid=0
        var duplicate=0
        customersList.forEach{
            val p=normalizePhone(it.phone)
            if(p.length<10) invalid++
            else if(!seen.add(p)) duplicate++
            else valid++
        }
        return arrayOf(valid,invalid,duplicate,(customersList.size-currentCampaignIndex).coerceAtLeast(0))
    }

    private fun dashboard(){
        buildNav("الرئيسية")
        clear()
        content.addView(tv("لوحة التحكم",24f,true))
        content.addView(tv("Bright Bulk Pro • إدارة العملاء والحملات من مكان واحد.",14f))
        card("إجمالي العملاء",customersList.size.toString())
        card("أرقام صالحة",statValue()[0].toString())
        card("أرقام غير صالحة",statValue()[1].toString())
        card("أرقام مكررة",statValue()[2].toString())
        card("تم التأكيد كمرسل",sent.toString())
        card("فشل",failed.toString())
        card("المتبقي",statValue()[3].toString())
        card("وجود WhatsApp","غير متحقق")
        content.addView(tv("ملاحظة: لا يمكن للتطبيق العادي تأكيد تسجيل الرقم على WhatsApp قبل فتحه، لذلك لا نعرض نتيجة غير مؤكدة كأنها حقيقة.",13f))
    }

    private fun customers(){
        buildNav("العملاء")
        clear()
        content.addView(tv("العملاء",24f,true))
        content.addView(tv("استورد قائمتك، وسيتم حفظها على الجهاز.",14f))

        val imp=btn("📂 استيراد CSV")
        imp.setOnClickListener{
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{
                type="text/csv"
                addCategory(Intent.CATEGORY_OPENABLE)
            },100)
        }
        content.addView(imp)

        val clr=btn("🗑 مسح قائمة العملاء")
        clr.setOnClickListener{
            customersList.clear()
            prefs.edit().remove("customers").apply()
            sent=0
            failed=0
            skipped=0
            currentCampaignIndex=0
            customers()
        }
        content.addView(clr)

        val st=statValue()
        content.addView(tv("إجمالي: ${customersList.size} • صالح: ${st[0]} • غير صالح: ${st[1]} • مكرر: ${st[2]}",16f,true))

        customersList.take(100).forEachIndexed{i,c->
            content.addView(tv("${i+1}. ${c.name.ifBlank{"بدون اسم"}} — ${c.phone}",14f))
        }

        if(customersList.size>100)
            content.addView(tv("يتم عرض أول 100 فقط.",13f))
    }

    private fun campaign(){
        buildNav("الحملة")
        clear()

        content.addView(tv("إنشاء حملة",24f,true))
        content.addView(tv("رسالة محفوظة + قوالب + مرفقات + تأخير يدوي احترافي.",14f))

        content.addView(tv("القناة",13f,true))
        val channel=Spinner(this)
        channel.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,listOf("WhatsApp Business","WhatsApp العادي"))

        val savedChannel=prefs.getString("channel","WhatsApp Business") ?: "WhatsApp Business"
        channel.setSelection(if(savedChannel=="WhatsApp العادي")1 else 0)
        campaignChannel=savedChannel

        channel.onItemSelectedListener=object:AdapterView.OnItemSelectedListener{
            override fun onItemSelected(parent:AdapterView<*>?,view:View?,position:Int,id:Long){
                campaignChannel=if(position==0)"WhatsApp Business" else "WhatsApp العادي"
                prefs.edit().putString("channel",campaignChannel).apply()
            }
            override fun onNothingSelected(parent:AdapterView<*>?){}
        }
        content.addView(channel)

        content.addView(tv("القالب",13f,true))
        val names=mutableListOf("بدون قالب")
        for(i in 1..5){
            val n=prefs.getString("template_${i}_name","") ?: ""
            if(n.isNotBlank()) names.add(n)
        }

        val templates=Spinner(this)
        templates.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,names)
        content.addView(templates)

        val msg=field("نص الرسالة — استخدم {{name}}",prefs.getString("draft_message","") ?: "",6)
        campaignMessage=msg.text.toString()
        content.addView(msg)

        val saveDraft=btn("💾 حفظ الرسالة الحالية")
        saveDraft.setOnClickListener{
            campaignMessage=msg.text.toString()
            prefs.edit().putString("draft_message",campaignMessage).apply()
            toast("تم حفظ الرسالة")
        }
        content.addView(saveDraft)

        val templateName=field("اسم القالب للحفظ")
        content.addView(templateName)

        val saveTemplate=btn("➕ حفظ كقالب")
        saveTemplate.setOnClickListener{
            val n=templateName.text.toString().trim()
            val text=msg.text.toString()

            if(n.isBlank()||text.isBlank()){
                toast("اكتب اسم القالب والرسالة")
                return@setOnClickListener
            }

            var slot=0
            for(i in 1..5){
                if((prefs.getString("template_${i}_name","") ?: "").isBlank()){
                    slot=i
                    break
                }
            }

            if(slot==0){
                toast("القوالب الخمسة ممتلئة")
                return@setOnClickListener
            }

            prefs.edit()
                .putString("template_${slot}_name",n)
                .putString("template_${slot}_text",text)
                .apply()

            toast("تم حفظ القالب")
            campaign()
        }
        content.addView(saveTemplate)

        templates.onItemSelectedListener=object:AdapterView.OnItemSelectedListener{
            override fun onItemSelected(parent:AdapterView<*>?,view:View?,position:Int,id:Long){
                if(position==0)return

                val selectedName=names[position]
                var slot=0

                for(i in 1..5){
                    if((prefs.getString("template_${i}_name","") ?: "")==selectedName){
                        slot=i
                        break
                    }
                }

                if(slot>0){
                    val t=prefs.getString("template_${slot}_text","") ?: ""
                    msg.setText(t)
                    campaignMessage=t
                    prefs.edit().putString("draft_message",t).apply()
                }
            }

            override fun onNothingSelected(parent:AdapterView<*>?){}
        }

        content.addView(tv("التأخير بين العملاء",13f,true))

        val delay=Spinner(this)
        val delays=listOf("5 ثواني","10 ثواني","15 ثانية","30 ثانية")
        delay.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,delays)

        val savedDelay=prefs.getInt("delay_seconds",10)
        delaySeconds=savedDelay
        delay.setSelection(listOf(5,10,15,30).indexOf(savedDelay).coerceAtLeast(0))

        delay.onItemSelectedListener=object:AdapterView.OnItemSelectedListener{
            override fun onItemSelected(parent:AdapterView<*>?,view:View?,position:Int,id:Long){
                delaySeconds=listOf(5,10,15,30)[position]
                prefs.edit().putInt("delay_seconds",delaySeconds).apply()
            }
            override fun onNothingSelected(parent:AdapterView<*>?){}
        }
        content.addView(delay)

        val attach=btn("📎 اختيار مرفق — صورة / PDF / مستند / فيديو")
        attach.setOnClickListener{
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{
                type="*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            },200)
        }
        content.addView(attach)

        val attachInfo=tv(if(attachmentUri==null)"لا يوجد مرفق" else "📎 $attachmentName",13f)
        content.addView(attachInfo)

        val remove=btn("✕ إزالة المرفق")
        remove.setOnClickListener{
            attachmentUri=null
            attachmentName=""
            attachInfo.text="لا يوجد مرفق"
        }
        content.addView(remove)

        val check=btn("🔍 فحص القائمة")
        check.setOnClickListener{showPreflight()}
        content.addView(check)

        val start=btn("🚀 فتح أول عميل")
        start.setOnClickListener{
            if(customersList.isEmpty()){
                toast("استورد العملاء أولاً")
                return@setOnClickListener
            }

            campaignMessage=msg.text.toString()
            prefs.edit().putString("draft_message",campaignMessage).apply()

            currentCampaignIndex=0
            sent=0
            failed=0
            skipped=0

            openCampaignCustomer()
        }
        content.addView(start)

        nextButton=btn("✉️ تم الإرسال — انتظار ${delaySeconds} ث")
        nextButton!!.isEnabled=false
        nextButton!!.setOnClickListener{confirmSentAndNext()}
        content.addView(nextButton!!)

        val reset=btn("🔄 إعادة الحملة من البداية")
        reset.setOnClickListener{
            cancelCountdown()
            currentCampaignIndex=0
            sent=0
            failed=0
            skipped=0
            status.text="تمت إعادة الحملة"
            updateManualStats()
        }
        content.addView(reset)

        stats=tv("جاهز — ${customersList.size} عميل",15f,true)
        content.addView(stats)

        content.addView(tv(
            "المرفق يستخدم Android Sharesheet. عند مشاركة ملف، قد يطلب WhatsApp منك اختيار المحادثة؛ Android لا يضمن توجيه المرفق إلى رقم محدد.",
            13f
        ))
    }

    private fun showPreflight(){
        if(customersList.isEmpty()){
            toast("استورد العملاء أولاً")
            return
        }

        val st=statValue()

        AlertDialog.Builder(this)
            .setTitle("مراجعة الحملة")
            .setMessage(
                "إجمالي: ${customersList.size}\n" +
                "أرقام صالحة: ${st[0]}\n" +
                "غير صالحة: ${st[1]}\n" +
                "مكررة: ${st[2]}\n\n" +
                "وجود WhatsApp: غير متحقق — سيتم فتح الرقم عند البدء."
            )
            .setPositiveButton("حسنًا",null)
            .show()
    }

    private fun openCampaignCustomer(){
        if(currentCampaignIndex>=customersList.size){
            status.text="انتهت الحملة"
            toast("انتهت الحملة")
            return
        }

        val c=customersList[currentCampaignIndex]
        val phone=normalizePhone(c.phone)

        if(phone.length<10){
            failed++
            currentCampaignIndex++
            updateManualStats()
            toast("رقم غير صالح: ${c.name}")
            return
        }

        val text=campaignMessage.replace(
            "{{name}}",
            c.name.ifBlank{"عميلنا"}
        )

        try{
            if(attachmentUri==null){
                val uri=Uri.parse("https://wa.me/$phone?text=${Uri.encode(text)}")
                val intent=Intent(Intent.ACTION_VIEW,uri).apply{
                    setPackage(
                        if(campaignChannel=="WhatsApp Business")
                            "com.whatsapp.w4b"
                        else
                            "com.whatsapp"
                    )
                }
                startActivity(intent)
            }else{
                val share=Intent(Intent.ACTION_SEND).apply{
                    type=contentResolver.getType(attachmentUri!!) ?: "*/*"
                    putExtra(Intent.EXTRA_STREAM,attachmentUri)
                    putExtra(Intent.EXTRA_TEXT,text)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    setPackage(
                        if(campaignChannel=="WhatsApp Business")
                            "com.whatsapp.w4b"
                        else
                            "com.whatsapp"
                    )
                }
                startActivity(share)
            }

            status.text="محادثة العميل ${currentCampaignIndex+1} مفتوحة"

            if(nextButton!=null)
                nextButton!!.isEnabled=true

        }catch(e:Exception){
            failed++
            currentCampaignIndex++
            updateManualStats()
            toast("تعذر فتح WhatsApp أو مشاركة الملف")
        }
    }

    private fun confirmSentAndNext(){
        if(currentCampaignIndex>=customersList.size){
            toast("انتهت الحملة")
            return
        }

        sent++

        nextButton?.isEnabled=false

        var left=delaySeconds
        nextButton?.text="⏳ التالي بعد ${left} ث"

        countdownRunnable=object:Runnable{
            override fun run(){
                left--

                if(left<=0){
                    nextButton?.text="➡️ فتح العميل التالي"
                    nextButton?.isEnabled=true

                    currentCampaignIndex++
                    updateManualStats()

                    if(currentCampaignIndex<customersList.size){
                        openCampaignCustomer()
                    }else{
                        status.text="اكتملت الحملة"
                        toast("اكتملت الحملة")
                    }
                }else{
                    nextButton?.text="⏳ التالي بعد ${left} ث"
                    handler.postDelayed(this,1000)
                }
            }
        }

        handler.postDelayed(countdownRunnable!!,1000)
    }

    private fun cancelCountdown(){
        countdownRunnable?.let{handler.removeCallbacks(it)}
        countdownRunnable=null
    }

    private fun updateManualStats(){
        runOnUiThread{
            stats.text="التقدم: ${currentCampaignIndex}/${customersList.size} • تم التأكيد: ${sent} • فشل: ${failed}"
            status.text=if(currentCampaignIndex<customersList.size)"جاهز للعميل التالي" else "اكتملت الحملة"
        }
    }

    private fun settings(){
        buildNav("الإعدادات")
        clear()

        content.addView(tv("الإعدادات",24f,true))
        content.addView(tv(
            "Bright Bulk Pro يعمل بوضع Manual: يجهز المحادثة، وأنت تؤكد الإرسال من WhatsApp.",
            14f
        ))

        val wa=btn("🟢 WhatsApp العادي")
        wa.setOnClickListener{
            prefs.edit().putString("channel","WhatsApp العادي").apply()
            campaignChannel="WhatsApp العادي"
            toast("تم اختيار WhatsApp العادي")
        }
        content.addView(wa)

        val wab=btn("🟢 WhatsApp Business")
        wab.setOnClickListener{
            prefs.edit().putString("channel","WhatsApp Business").apply()
            campaignChannel="WhatsApp Business"
            toast("تم اختيار WhatsApp Business")
        }
        content.addView(wab)

        content.addView(tv("المظهر: Graphite / Black • Dark بالكامل.",14f))
        content.addView(tv(
            "فحص وجود WhatsApp الحقيقي يحتاج مصدرًا رسميًا من WhatsApp/Meta؛ التطبيق لا يعرض نتيجة غير مؤكدة كأنها حقيقة.",
            13f
        ))
    }

    private fun testConnection(){
        val phoneId=prefs.getString("wa_phone_id","")?.trim()?:""
        val token=prefs.getString("wa_token","")?.trim()?:""
        if(phoneId.isBlank()||token.isBlank()){
            toast("أدخل Phone Number ID وAccess Token أولاً")
            return
        }
        status.text="جارٍ اختبار Meta..."
        thread{
            val r=apiGet("https://graph.facebook.com/v26.0/$phoneId?fields=id,display_phone_number,verified_name",token,true)
            runOnUiThread{
                val ok=r.first in 200..299
                status.text=if(ok)"✅ Meta متصل" else "❌ فشل اتصال Meta: HTTP ${r.first}"
                toast(if(ok)"تم الاتصال بـ Meta بنجاح" else r.second.take(180))
            }
        }
    }

    private fun updateStats(done:Int,total:Int){runOnUiThread{stats.text="التقدم: $done/$total — نجاح: $sent — فشل: $failed — تخطي: $skipped";status.text="جارٍ الإرسال: $done/$total"}}
    private fun buildOpenWaJson(phone:String,text:String)="""{"to":"${esc(phone)}@c.us","text":"${esc(text)}"}"""
    private fun esc(s:String)=s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r")
    private fun normalizePhone(raw:String):String{var s=raw.trim().replace(" ","").replace("-","").replace("(","").replace(")","");if(s.startsWith("+"))s=s.drop(1);if(s.startsWith("00"))s=s.drop(2);if(s.startsWith("01")&&s.length==11)s="20"+s.drop(1);return s.filter{it.isDigit()}}
    private fun apiGet(url:String,key:String,bearer:Boolean=false):Pair<Int,String>{
        return try{
            val c=URL(url).openConnection() as HttpURLConnection
            c.requestMethod="GET"
            if(key.isNotBlank()) c.setRequestProperty(if(bearer)"Authorization" else "X-API-Key",if(bearer)"Bearer $key" else key)
            c.connectTimeout=20000
            c.readTimeout=20000
            val code=c.responseCode
            val st=if(code>=400)c.errorStream else c.inputStream
            val t=st?.let{BufferedReader(InputStreamReader(it)).readText()}?:""
            c.disconnect()
            code to t
        }catch(e:Exception){-1 to(e.message?:"Network error")}
    }

    private fun apiPost(url:String,key:String,json:String,bearer:Boolean=false):Pair<Int,String>{
        return try{
            val c=URL(url).openConnection() as HttpURLConnection
            c.requestMethod="POST"
            c.doOutput=true
            if(key.isNotBlank()) c.setRequestProperty(if(bearer)"Authorization" else "X-API-Key",if(bearer)"Bearer $key" else key)
            c.setRequestProperty("Content-Type","application/json; charset=UTF-8")
            c.connectTimeout=20000
            c.readTimeout=20000
            c.outputStream.use{it.write(json.toByteArray(StandardCharsets.UTF_8))}
            val code=c.responseCode
            val st=if(code>=400)c.errorStream else c.inputStream
            val t=st?.let{BufferedReader(InputStreamReader(it)).readText()}?:""
            c.disconnect()
            code to t
        }catch(e:Exception){-1 to(e.message?:"Network error")}
    }

    private fun toast(s:String)=runOnUiThread{Toast.makeText(this,s,Toast.LENGTH_SHORT).show()}
    override fun onActivityResult(req:Int,res:Int,data:Intent?){
        super.onActivityResult(req,res,data)

        if(res!=RESULT_OK)return

        if(req==200){
            attachmentUri=data?.data
            attachmentName=attachmentUri?.let{queryDisplayName(it)} ?: "مرفق"
            toast("تم اختيار: $attachmentName")
            return
        }

        if(req!=100)return

        val uri:Uri=data?.data ?: return

        try{
            contentResolver.openInputStream(uri)?.use{input->
                val lines=BufferedReader(InputStreamReader(input)).readLines()
                val parsed=parseCsv(lines)

                customersList.clear()
                customersList.addAll(parsed.distinctBy{it.phone})
                saveCustomers()

                status.text="تم استيراد ${customersList.size} عميل"
                customers()
            }
        }catch(e:Exception){
            status.text="تعذر قراءة CSV: ${e.message}"
        }
    }

    private fun queryDisplayName(uri:Uri):String{
        var name="مرفق"

        try{
            contentResolver.query(
                uri,
                arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use{c->
                if(c.moveToFirst()){
                    val i=c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if(i>=0)name=c.getString(i)
                }
            }
        }catch(_:Exception){}

        return name
    }

    private fun saveCustomers(){
        val raw=customersList.joinToString("\n"){
            it.name.replace("|"," ")+"|"+it.phone.replace("|"," ")
        }

        prefs.edit().putString("customers",raw).apply()
    }

    private fun loadCustomers(){
        val raw=prefs.getString("customers","") ?: ""
        if(raw.isBlank())return

        raw.lines().forEach{line->
            val p=line.split("|",limit=2)

            if(p.size==2&&p[1].isNotBlank())
                customersList.add(Customer(p[0],p[1]))
        }
    }

    private fun parseCsv(lines:List<String>):List<Customer>{if(lines.isEmpty())return emptyList();val first=splitCsv(lines.first()).map{it.trim().lowercase(Locale.ROOT)};val ni0=first.indexOfFirst{it in setOf("name","الاسم","customer","customer_name")};val pi0=first.indexOfFirst{it in setOf("phone","mobile","telephone","رقم","رقم الهاتف","whatsapp")};val start=if(ni0>=0||pi0>=0)1 else 0;val ni=if(ni0>=0)ni0 else 0;val pi=if(pi0>=0)pi0 else 1;return lines.drop(start).mapNotNull{val c=splitCsv(it);if(c.size<=maxOf(ni,pi))null else Customer(c[ni].trim(),c[pi].trim()).takeIf{x->x.phone.isNotBlank()}}}
    private fun splitCsv(line:String):List<String>{val o=mutableListOf<String>();val b=StringBuilder();var q=false;for(ch in line){when{ch=='"'->q=!q;ch==','&&!q->{o.add(b.toString());b.setLength(0)}else->b.append(ch)}};o.add(b.toString());return o}
}
