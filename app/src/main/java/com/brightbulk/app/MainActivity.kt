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
    private lateinit var root: LinearLayout
    private lateinit var status: TextView
    private lateinit var content: LinearLayout
    private lateinit var stats: TextView

    private fun dp(v:Int)= (v*resources.displayMetrics.density).toInt()
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
    private fun dashboard(){buildNav("الرئيسية");clear();content.addView(tv("لوحة التحكم",22f,true));content.addView(tv("إدارة العملاء والحملات والإرسال من مكان واحد.",14f));card("إجمالي العملاء",customersList.size.toString());card("تم الإرسال",sent.toString());card("فشل",failed.toString());card("تم التخطي",skipped.toString());content.addView(tv("الاتصال الرسمي: WhatsApp Business Cloud API\nأدخل Phone Number ID وAccess Token من Meta في الإعدادات.",14f))}
    private fun customers(){buildNav("العملاء");clear();content.addView(tv("العملاء",22f,true));content.addView(tv("الاسم ورقم الهاتف مطلوبان للإرسال.",14f));val imp=btn("📂 استيراد CSV");imp.setOnClickListener{startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{type="text/*";addCategory(Intent.CATEGORY_OPENABLE)},100)};content.addView(imp);val clr=btn("🗑 مسح قائمة العملاء");clr.setOnClickListener{customersList.clear();sent=0;failed=0;skipped=0;customers()};content.addView(clr);content.addView(tv("عدد العملاء: ${customersList.size}",17f,true));customersList.take(50).forEachIndexed{i,c->content.addView(tv("${i+1}. ${c.name.ifBlank{"بدون اسم"}} — ${c.phone}",14f))};if(customersList.size>50)content.addView(tv("يتم عرض أول 50 فقط.",13f))}
    private fun campaign(){buildNav("الحملة");
        clear();content.addView(tv("إنشاء حملة",22f,true));content.addView(tv("نص مخصص بالاسم أو WhatsApp Template.",14f))
        val mode=Spinner(this);mode.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,arrayOf("رسالة نصية","WhatsApp Template"));content.addView(mode)
        val msg=field("نص الرسالة — استخدم {{name}}","",6);content.addView(msg)
        val tn=field("اسم القالب في Meta");val tl=field("لغة القالب مثل ar أو en_US","ar");val tp=field("متغيرات القالب مفصولة بفاصلة، مثال: أحمد,123","",3);tn.visibility=View.GONE;tl.visibility=View.GONE;tp.visibility=View.GONE;content.addView(tn);content.addView(tl);content.addView(tp)
        mode.onItemSelectedListener=object:AdapterView.OnItemSelectedListener{override fun onNothingSelected(p:AdapterView<*>?){ }override fun onItemSelected(p:AdapterView<*>?,v:View?,pos:Int,id:Long){val z=pos==1;msg.visibility=if(z)View.GONE else View.VISIBLE;tn.visibility=if(z)View.VISIBLE else View.GONE;tl.visibility=if(z)View.VISIBLE else View.GONE;tp.visibility=if(z)View.VISIBLE else View.GONE}}
        val delay=field("التأخير بين الرسائل بالثواني","3");delay.inputType=2;content.addView(delay)
        val test=btn("🧪 إرسال اختبار لأول عميل");test.setOnClickListener{if(customersList.isEmpty())toast("استورد العملاء أولاً")else sendCampaign(listOf(customersList.first()),msg.text.toString(),tn.text.toString(),tl.text.toString(),tp.text.toString(),delay.text.toString().toLongOrNull()?:3L,mode.selectedItemPosition==1)};content.addView(test)
        val start=btn("🚀 بدء الحملة");start.setOnClickListener{if(customersList.isEmpty()){toast("استورد العملاء أولاً");return@setOnClickListener};sent=0;failed=0;skipped=0;sendCampaign(customersList.toList(),msg.text.toString(),tn.text.toString(),tl.text.toString(),tp.text.toString(),delay.text.toString().toLongOrNull()?:3L,mode.selectedItemPosition==1)};content.addView(start)
        val stop=btn("⏹ إيقاف الإرسال");stop.setOnClickListener{running=false;status.text="تم طلب إيقاف الحملة."};content.addView(stop);stats=tv("جاهز — ${customersList.size} عميل",15f,true);content.addView(stats)
    }
    private fun settings(){buildNav("الإعدادات");
        clear();content.addView(tv("إعدادات WhatsApp",22f,true));content.addView(tv("أدخل بيانات Cloud API. لا تشارك Access Token.",14f))
        val pid=field("Phone Number ID",prefs.getString("phone_id","")?:"");val tok=field("Access Token",prefs.getString("token","")?:"");tok.inputType=0x81;val ver=field("Graph API Version",prefs.getString("version","v23.0")?:"v23.0");content.addView(pid);content.addView(tok);content.addView(ver)
        val save=btn("💾 حفظ الإعدادات");save.setOnClickListener{prefs.edit().putString("phone_id",pid.text.toString().trim()).putString("token",tok.text.toString().trim()).putString("version",ver.text.toString().trim().ifBlank{"v23.0"}).apply();status.text="تم حفظ إعدادات الاتصال";toast("تم الحفظ")};content.addView(save)
        val test=btn("🔌 اختبار اتصال WhatsApp");test.setOnClickListener{save.performClick();testConnection()};content.addView(test)
        content.addView(tv("المطلوب من Meta:\n• WhatsApp Business Account\n• Phone Number ID\n• Access Token بصلاحية whatsapp_business_messaging\n• Webhook لاحقًا لتحديث delivered/read/failed.",14f))
    }
    private fun testConnection(){val pid=prefs.getString("phone_id","")?: "";val tok=prefs.getString("token","")?: "";val ver=prefs.getString("version","v23.0")?:"v23.0";if(pid.isBlank()||tok.isBlank()){toast("أدخل Phone Number ID وAccess Token");return};status.text="جارٍ اختبار الاتصال...";thread{val r=apiGet("https://graph.facebook.com/$ver/$pid?fields=display_phone_number,verified_name",tok);runOnUiThread{status.text=if(r.first in 200..299)"✅ الاتصال ناجح" else "❌ فشل الاتصال: HTTP ${r.first}";toast(if(r.first in 200..299) "WhatsApp Cloud API متصل" else r.second.take(120))}}}
    private fun sendCampaign(list:List<Customer>,message:String,templateName:String,lang:String,rawParams:String,delaySeconds:Long,templateMode:Boolean){
        if(running){toast("هناك حملة تعمل بالفعل");return};val pid=prefs.getString("phone_id","")?: "";val tok=prefs.getString("token","")?: "";val ver=prefs.getString("version","v23.0")?:"v23.0";if(pid.isBlank()||tok.isBlank()){toast("ادخل إعدادات WhatsApp أولاً");settings();return};running=true;status.text="🚀 الحملة تعمل...";thread{for((i,c)in list.withIndex()){if(!running)break;val phone=normalizePhone(c.phone);if(phone.isBlank()){skipped++;updateStats(i+1,list.size);continue};val body=if(templateMode)buildTemplateJson(phone,templateName.trim(),lang.trim().ifBlank{"ar"},rawParams)else buildTextJson(phone,message.replace("{{name}}",c.name.ifBlank{"عميلنا"}));val r=apiPost("https://graph.facebook.com/$ver/$pid/messages",tok,body);if(r.first in 200..299)sent++else failed++;updateStats(i+1,list.size);if(i<list.lastIndex&&running)Thread.sleep(delaySeconds.coerceAtLeast(0)*1000L)};running=false;runOnUiThread{status.text="انتهت الحملة — نجاح: $sent | فشل: $failed | تخطي: $skipped";toast("انتهى الإرسال")}}
    }
    private fun updateStats(done:Int,total:Int){runOnUiThread{stats.text="التقدم: $done/$total — نجاح: $sent — فشل: $failed — تخطي: $skipped";status.text="جارٍ الإرسال: $done/$total"}}
    private fun buildTextJson(phone:String,text:String)="""{"messaging_product":"whatsapp","recipient_type":"individual","to":"${esc(phone)}","type":"text","text":{"preview_url":true,"body":"${esc(text)}"}}"""
    private fun buildTemplateJson(phone:String,name:String,language:String,raw:String):String{val vals=raw.split(",").map{it.trim()}.filter{it.isNotEmpty()};val ps=vals.joinToString(","){"""{"type":"text","text":"${esc(it)}"}"""};val comp=if(ps.isBlank())"" else """,\"components\":[{\"type\":\"body\",\"parameters\":[$ps]}]""";return """{"messaging_product":"whatsapp","to":"${esc(phone)}","type":"template","template":{"name":"${esc(name)}","language":{"code":"${esc(language)}"}$comp}}"""}
    private fun esc(s:String)=s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r")
    private fun normalizePhone(raw:String):String{var s=raw.trim().replace(" ","").replace("-","").replace("(","").replace(")","");if(s.startsWith("+"))s=s.drop(1);if(s.startsWith("00"))s=s.drop(2);if(s.startsWith("01")&&s.length==11)s="20"+s.drop(1);return s.filter{it.isDigit()}}
    private fun apiGet(url:String,token:String):Pair<Int,String>{return try{val c=URL(url).openConnection()as HttpURLConnection;c.requestMethod="GET";c.setRequestProperty("Authorization","Bearer $token");c.connectTimeout=20000;c.readTimeout=20000;val code=c.responseCode;val st=if(code>=400)c.errorStream else c.inputStream;val t=st?.let{BufferedReader(InputStreamReader(it)).readText()}?:"";c.disconnect();code to t}catch(e:Exception){-1 to(e.message?:"Network error")}}
    private fun apiPost(url:String,token:String,json:String):Pair<Int,String>{return try{val c=URL(url).openConnection()as HttpURLConnection;c.requestMethod="POST";c.doOutput=true;c.setRequestProperty("Authorization","Bearer $token");c.setRequestProperty("Content-Type","application/json; charset=UTF-8");c.connectTimeout=20000;c.readTimeout=20000;c.outputStream.use{it.write(json.toByteArray(StandardCharsets.UTF_8))};val code=c.responseCode;val st=if(code>=400)c.errorStream else c.inputStream;val t=st?.let{BufferedReader(InputStreamReader(it)).readText()}?:"";c.disconnect();code to t}catch(e:Exception){-1 to(e.message?:"Network error")}}
    private fun toast(s:String)=runOnUiThread{Toast.makeText(this,s,Toast.LENGTH_SHORT).show()}
    override fun onActivityResult(req:Int,res:Int,data:Intent?){super.onActivityResult(req,res,data);if(req!=100||res!=RESULT_OK)return;val uri:Uri=data?.data?:return;try{contentResolver.openInputStream(uri)?.use{input->val lines=BufferedReader(InputStreamReader(input)).readLines();val parsed=parseCsv(lines);customersList.clear();customersList.addAll(parsed.distinctBy{it.phone});status.text="تم استيراد ${customersList.size} عميل";customers()}}catch(e:Exception){status.text="تعذر قراءة CSV: ${e.message}"}}
    private fun parseCsv(lines:List<String>):List<Customer>{if(lines.isEmpty())return emptyList();val first=splitCsv(lines.first()).map{it.trim().lowercase(Locale.ROOT)};val ni0=first.indexOfFirst{it in setOf("name","الاسم","customer","customer_name")};val pi0=first.indexOfFirst{it in setOf("phone","mobile","telephone","رقم","رقم الهاتف","whatsapp")};val start=if(ni0>=0||pi0>=0)1 else 0;val ni=if(ni0>=0)ni0 else 0;val pi=if(pi0>=0)pi0 else 1;return lines.drop(start).mapNotNull{val c=splitCsv(it);if(c.size<=maxOf(ni,pi))null else Customer(c[ni].trim(),c[pi].trim()).takeIf{x->x.phone.isNotBlank()}}}
    private fun splitCsv(line:String):List<String>{val o=mutableListOf<String>();val b=StringBuilder();var q=false;for(ch in line){when{ch=='"'->q=!q;ch==','&&!q->{o.add(b.toString());b.setLength(0)}else->b.append(ch)}};o.add(b.toString());return o}
}
