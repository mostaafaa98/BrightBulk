package com.brightbulk.app

import android.app.Activity
import android.os.Bundle
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
    private lateinit var root: LinearLayout
    private lateinit var status: TextView
    private lateinit var content: LinearLayout
    private lateinit var stats: TextView

    private fun dp(v:Int)= (v*resources.displayMetrics.density).toInt()
    private fun rounded(fill:Int,radius:Int,stroke:Int=Color.TRANSPARENT)=GradientDrawable().apply{shape=GradientDrawable.RECTANGLE;cornerRadius=dp(radius).toFloat();setColor(fill);if(stroke!=Color.TRANSPARENT)setStroke(dp(1),stroke)}
    private fun gradient(start:Int,end:Int,radius:Int)=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(start,end)).apply{cornerRadius=dp(radius).toFloat()}
    private fun tv(t:String,s:Float=15f,b:Boolean=false)=TextView(this).apply{text=t;textSize=s;setTextColor(Color.rgb(16,24,40));if(b)typeface=Typeface.DEFAULT_BOLD;setPadding(dp(3),dp(5),dp(3),dp(5))}
    private fun btn(t:String)=TextView(this).apply{text=t;textSize=14f;gravity=Gravity.CENTER;setTextColor(Color.rgb(8,123,80));typeface=Typeface.DEFAULT_BOLD;background=rounded(Color.WHITE,17,Color.rgb(174,226,207));setPadding(dp(12),dp(10),dp(12),dp(10));isClickable=true;minHeight=dp(48);elevation=dp(1).toFloat()}
    private fun field(h:String,v:String="",lines:Int=1)=EditText(this).apply{hint=h;setText(v);setPadding(dp(14),dp(10),dp(14),dp(10));if(lines>1){minLines=lines;gravity=Gravity.TOP}}

    override fun onCreate(b:Bundle?){super.onCreate(b);showApp()}
    private fun showApp(){
        root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(Color.rgb(246,248,251));layoutDirection=View.LAYOUT_DIRECTION_RTL}
        val head=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(18),dp(14),dp(18),dp(12));setBackgroundColor(Color.WHITE);elevation=dp(5).toFloat()}
        val logo=TextView(this).apply{text="B";textSize=22f;typeface=Typeface.DEFAULT_BOLD;gravity=Gravity.CENTER;setTextColor(Color.WHITE);background=gradient(Color.rgb(8,123,80),Color.rgb(10,168,107),18);elevation=dp(4).toFloat()}
        head.addView(logo,LinearLayout.LayoutParams(dp(48),dp(48)))
        val brand=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(12),0,0,0)}
        brand.addView(tv("BRIGHT BULK",19f,true));brand.addView(tv("WhatsApp Campaign Manager",12f))
        head.addView(brand,LinearLayout.LayoutParams(0,-2,1f))
        status=tv("● جاهز",12f,true);status.setTextColor(Color.rgb(8,123,80));status.background=rounded(Color.rgb(232,250,242),18);status.setPadding(dp(12),dp(7),dp(12),dp(7));head.addView(status)
        root.addView(head)
        logo.animate().scaleX(1.08f).scaleY(1.08f).setDuration(650).setInterpolator(AccelerateDecelerateInterpolator()).withEndAction{logo.animate().scaleX(1f).scaleY(1f).setDuration(650).start()}.start()
        val scroll=ScrollView(this).apply{isFillViewport=true};content=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(16),dp(16),dp(24));layoutDirection=View.LAYOUT_DIRECTION_RTL};scroll.addView(content);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        nav=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER;setPadding(dp(8),dp(8),dp(8),dp(9));background=rounded(Color.WHITE,24);elevation=dp(12).toFloat();layoutDirection=View.LAYOUT_DIRECTION_RTL}
        root.addView(nav,LinearLayout.LayoutParams(-1,dp(76)));setContentView(root);dashboard()
    }
    private lateinit var nav: LinearLayout
    private fun buildNav(active:String){nav.removeAllViews();listOf("الرئيسية" to "⌂","العملاء" to "♙","الحملة" to "✦","الإعدادات" to "⚙").forEach{(label,icon)->val selected=label==active;val x=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER;background=rounded(if(selected) Color.rgb(232,250,242) else Color.TRANSPARENT,18);setPadding(dp(6),dp(3),dp(6),dp(3));setOnClickListener{when(label){"الرئيسية"->dashboard();"العملاء"->customers();"الحملة"->campaign();"الإعدادات"->settings()}}};x.addView(tv(icon,19f,true).apply{setTextColor(if(selected) Color.rgb(10,168,107) else Color.rgb(102,112,133));setGravity(Gravity.CENTER)});x.addView(tv(label,10f,selected).apply{setTextColor(if(selected) Color.rgb(8,123,80) else Color.rgb(102,112,133));setGravity(Gravity.CENTER)});nav.addView(x,LinearLayout.LayoutParams(0,-1,1f))}}
    private fun clear(){content.removeAllViews();content.scrollTo(0,0)}
    private fun card(t:String,v:String){val x=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(12),dp(16),dp(12));setBackground(rounded(Color.WHITE,20));elevation=dp(2).toFloat()};x.addView(tv(t,13f));x.addView(tv(v,24f,true));val p=LinearLayout.LayoutParams(-1,dp(92));p.setMargins(0,0,0,dp(10));content.addView(x,p);x.alpha=0f;x.translationY=dp(10).toFloat();x.animate().alpha(1f).translationY(0f).setDuration(260).start()}
    private fun dashboard(){buildNav("الرئيسية");clear();content.addView(tv("لوحة التحكم",22f,true));content.addView(tv("إدارة العملاء والحملات والإرسال من مكان واحد.",14f));card("إجمالي العملاء",customersList.size.toString());card("تم الإرسال",sent.toString());card("فشل",failed.toString());card("تم التخطي",skipped.toString());content.addView(tv("إرسال يدوي سريع عبر WhatsApp أو WhatsApp Business — بدون Cloud API.",14f))}
    private fun customers(){buildNav("العملاء");clear();content.addView(tv("العملاء",22f,true));content.addView(tv("الاسم ورقم الهاتف مطلوبان للإرسال.",14f));val imp=btn("📂 استيراد CSV");imp.setOnClickListener{startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{type="text/*";addCategory(Intent.CATEGORY_OPENABLE)},100)};content.addView(imp);val clr=btn("🗑 مسح قائمة العملاء");clr.setOnClickListener{customersList.clear();sent=0;failed=0;skipped=0;customers()};content.addView(clr);content.addView(tv("عدد العملاء: ${customersList.size}",17f,true));customersList.take(50).forEachIndexed{i,c->content.addView(tv("${i+1}. ${c.name.ifBlank{"بدون اسم"}} — ${c.phone}",14f))};if(customersList.size>50)content.addView(tv("يتم عرض أول 50 فقط.",13f))}
    private fun campaign(){
        buildNav("الحملة")
        clear()

        content.addView(tv("إنشاء حملة",22f,true))
        content.addView(tv("اختر القناة، ثم افتح كل عميل في WhatsApp والرسالة ستكون جاهزة للإرسال.",14f))

        val channel=Spinner(this)
        channel.adapter=ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("WhatsApp Business","WhatsApp العادي")
        )

        channel.onItemSelectedListener=object:AdapterView.OnItemSelectedListener{
            override fun onItemSelected(parent:AdapterView<*>?,view:View?,position:Int,id:Long){
                campaignChannel=if(position==0)"WhatsApp Business" else "WhatsApp العادي"
                prefs.edit().putString("channel",campaignChannel).apply()
            }
            override fun onNothingSelected(parent:AdapterView<*>?){}
        }

        content.addView(tv("قناة الإرسال",14f,true))
        content.addView(channel)

        val msg=field("نص الرسالة — استخدم {{name}}","",6)
        content.addView(msg)

        val start=btn("🚀 فتح أول عميل")
        start.setOnClickListener{
            if(customersList.isEmpty()){
                toast("استورد العملاء أولاً")
                return@setOnClickListener
            }
            campaignMessage=msg.text.toString()
            currentCampaignIndex=0
            sent=0
            failed=0
            skipped=0
            openCampaignCustomer()
        }
        content.addView(start)

        val next=btn("➡️ فتح العميل التالي")
        next.setOnClickListener{
            if(customersList.isEmpty()){
                toast("استورد العملاء أولاً")
                return@setOnClickListener
            }
            if(campaignMessage.isBlank())
                campaignMessage=msg.text.toString()

            if(currentCampaignIndex>=customersList.size){
                toast("انتهت الحملة")
                return@setOnClickListener
            }

            openCampaignCustomer()
        }
        content.addView(next)

        val reset=btn("🔄 إعادة الحملة من البداية")
        reset.setOnClickListener{
            currentCampaignIndex=0
            sent=0
            failed=0
            skipped=0
            status.text="تمت إعادة الحملة"
        }
        content.addView(reset)

        stats=tv("جاهز — ${customersList.size} عميل",15f,true)
        content.addView(stats)

        content.addView(tv(
            "بعد فتح المحادثة اضغط إرسال داخل WhatsApp، ثم ارجع للتطبيق واضغط «فتح العميل التالي».",
            14f
        ))
    }

    private fun openCampaignCustomer(){
        if(currentCampaignIndex>=customersList.size){
            status.text="انتهت الحملة"
            toast("انتهت الحملة")
            return
        }

        val c=customersList[currentCampaignIndex]
        val phone=normalizePhone(c.phone)

        if(phone.isBlank()){
            skipped++
            currentCampaignIndex++
            updateManualStats()
            return
        }

        val text=campaignMessage.replace(
            "{{name}}",
            c.name.ifBlank{"عميلنا"}
        )

        val uri=Uri.parse(
            "https://wa.me/$phone?text=${Uri.encode(text)}"
        )

        val intent=Intent(Intent.ACTION_VIEW,uri)

        intent.setPackage(
            if(campaignChannel=="WhatsApp Business")
                "com.whatsapp.w4b"
            else
                "com.whatsapp"
        )

        try{
            startActivity(intent)
            sent++
            currentCampaignIndex++
            updateManualStats()
        }catch(e:Exception){
            try{
                startActivity(Intent(Intent.ACTION_VIEW,uri))
                sent++
                currentCampaignIndex++
                updateManualStats()
            }catch(ex:Exception){
                failed++
                currentCampaignIndex++
                updateManualStats()
                toast("WhatsApp غير مثبت أو لم يتم العثور عليه")
            }
        }
    }

    private fun updateManualStats(){
        runOnUiThread{
            stats.text="التقدم: $currentCampaignIndex/${customersList.size} — تم فتح: $sent — تخطي: $skipped — فشل: $failed"
            status.text="العميل التالي جاهز"
        }
    }

    private fun settings(){
        buildNav("الإعدادات")
        clear()

        content.addView(tv("إعدادات الإرسال",22f,true))
        content.addView(tv(
            "Bright Bulk يفتح WhatsApp أو WhatsApp Business مباشرةً ويضع الرسالة داخل المحادثة.",
            14f
        ))
        content.addView(tv(
            "الإرسال النهائي يتم من داخل WhatsApp بالضغط على زر إرسال.",
            14f
        ))

        val wa=btn("🟢 استخدام WhatsApp العادي")
        wa.setOnClickListener{
            prefs.edit().putString("channel","WhatsApp العادي").apply()
            campaignChannel="WhatsApp العادي"
            toast("تم اختيار WhatsApp العادي")
        }
        content.addView(wa)

        val wab=btn("🟢 استخدام WhatsApp Business")
        wab.setOnClickListener{
            prefs.edit().putString("channel","WhatsApp Business").apply()
            campaignChannel="WhatsApp Business"
            toast("تم اختيار WhatsApp Business")
        }
        content.addView(wab)
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
    override fun onActivityResult(req:Int,res:Int,data:Intent?){super.onActivityResult(req,res,data);if(req!=100||res!=RESULT_OK)return;val uri:Uri=data?.data?:return;try{contentResolver.openInputStream(uri)?.use{input->val lines=BufferedReader(InputStreamReader(input)).readLines();val parsed=parseCsv(lines);customersList.clear();customersList.addAll(parsed.distinctBy{it.phone});status.text="تم استيراد ${customersList.size} عميل";customers()}}catch(e:Exception){status.text="تعذر قراءة CSV: ${e.message}"}}
    private fun parseCsv(lines:List<String>):List<Customer>{if(lines.isEmpty())return emptyList();val first=splitCsv(lines.first()).map{it.trim().lowercase(Locale.ROOT)};val ni0=first.indexOfFirst{it in setOf("name","الاسم","customer","customer_name")};val pi0=first.indexOfFirst{it in setOf("phone","mobile","telephone","رقم","رقم الهاتف","whatsapp")};val start=if(ni0>=0||pi0>=0)1 else 0;val ni=if(ni0>=0)ni0 else 0;val pi=if(pi0>=0)pi0 else 1;return lines.drop(start).mapNotNull{val c=splitCsv(it);if(c.size<=maxOf(ni,pi))null else Customer(c[ni].trim(),c[pi].trim()).takeIf{x->x.phone.isNotBlank()}}}
    private fun splitCsv(line:String):List<String>{val o=mutableListOf<String>();val b=StringBuilder();var q=false;for(ch in line){when{ch=='"'->q=!q;ch==','&&!q->{o.add(b.toString());b.setLength(0)}else->b.append(ch)}};o.add(b.toString());return o}
}
