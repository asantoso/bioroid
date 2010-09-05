<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	version="1.0" xmlns:android="http://schemas.android.com/apk/res/android"
	xmlns:fn="http://www.w3.org/2005/xpath-functions">
	<xsl:output method="text" />
	
	<xsl:variable name="linebreak">&#xA;</xsl:variable>
	
	<xsl:template match="/">
		<xsl:call-template name="gen.class">
			<xsl:with-param name="name" select="'MyActivity'"/>
			<xsl:with-param name="package" select="'com.neusou.android'"/>
		</xsl:call-template>
	</xsl:template>

	<xsl:template name="gen.class">
		<xsl:param name="name" select="MyActivity"></xsl:param>
		<xsl:param name="package" select="com.mycompany.android"></xsl:param>

	<xsl:value-of select="concat('package ',$package,';')" />

	<xsl:value-of select="concat('public class ',$name, ' extends Activity')" />{

		<xsl:variable name="views" select="//*[@android:id and local-name() != 'include']"></xsl:variable>

		<xsl:call-template name="gen.declareviewvars">
			<xsl:with-param name="views" select="$views" />
		</xsl:call-template>

		<xsl:call-template name="gen.oncreate">
		</xsl:call-template>

		<xsl:call-template name="gen.onpostcreate">
		</xsl:call-template>

		<xsl:call-template name="gen.ondestroy">
		</xsl:call-template>

		<xsl:call-template name="gen.onstart">
		</xsl:call-template>

		<xsl:call-template name="gen.onstop">
		</xsl:call-template>

		<xsl:call-template name="gen.onresume">
		</xsl:call-template>

		<xsl:call-template name="gen.onpause">
		</xsl:call-template>

		<xsl:call-template name="gen.onpostresume">
		</xsl:call-template>

		<xsl:call-template name="gen.onactivityresult">
		</xsl:call-template>

		<xsl:call-template name="gen.getintentextras">
		</xsl:call-template>

		<xsl:call-template name="gen.bindviews">
			<xsl:with-param name="views" select="$views" />
		</xsl:call-template>

		<xsl:call-template name="gen.initobjects">
		</xsl:call-template>

		<xsl:call-template name="gen.initviews">
		</xsl:call-template>

		<xsl:call-template name="gen.initbroadcastreceivers">
		</xsl:call-template>
		
		<xsl:call-template name="gen.doRegisterReceivers">
		</xsl:call-template>
		
		<xsl:call-template name="gen.doUnRegisterReceivers">
		</xsl:call-template>
		
		<xsl:call-template name="gen.resolvedata">
		</xsl:call-template>
		
}
	</xsl:template>

	<xsl:template name="gen.oncreate">
	<xsl:param name="layout"/>
	public void onCreate(Bundle savedInstanceState){
		setContentView(R.layout.<xsl:value-of select="$layout"/>);
		bindViews();
		getIntentExtras();
		initObjects();
		initViews();
		initBroadcastReceivers();	
	}
	</xsl:template>

	<xsl:template name="gen.declareviewvars">
		<xsl:param name="views" />
		<xsl:for-each select="$views">
	<xsl:variable name="name" select="substring-after(./@android:id,'/')" />
	<xsl:variable name="type" select="local-name()" />
	<xsl:value-of select="concat($type,' m',$name,';')" />
	<xsl:text>&#xA;</xsl:text>
		</xsl:for-each>
	</xsl:template>

	<xsl:template name="gen.bindviews">
		<xsl:param name="views" />
	public void bindViews(){
		<xsl:for-each select="$views">
			<xsl:variable name="name" select="substring-after(./@android:id,'/')" />
			<xsl:variable name="type" select="local-name()" />
			<xsl:value-of
				select="concat('m',$name,' = ','(',$type,')',' findViewById(R.id.',$name,');' )" />
			<xsl:text>&#xA;</xsl:text>
		</xsl:for-each>
	}
	</xsl:template>

	<xsl:template name="gen.onpostcreate">
	public void onPostCreate(){
		super.onPostCreate();
	}
	</xsl:template>

	<xsl:template name="gen.ondestroy">
	public void onDestroy(){
	 	super.onDestroy();
	}
	</xsl:template>

	<xsl:template name="gen.onstart">
	public void onStart(){
		super.onStart();
		doRegisterReceivers();
	}
	</xsl:template>

	<xsl:template name="gen.onstop">
	public void onStop(){
		super.onStop();
		doUnRegisterReceivers();
	}
	</xsl:template>

	<xsl:template name="gen.onresume">
	public void onResume(){
		super.onResume();
		resolveData();	 
	}
	</xsl:template>

	<xsl:template name="gen.onpause">
	public void onPause(){
	 	super.onPause();
	}
	</xsl:template>

	<xsl:template name="gen.onpostresume">
	public void onPostResume(){
	 	super.onPostResume();
	}
	</xsl:template>

	<xsl:template name="gen.onactivityresult">
	public void onActivityResult(int requestCode, int resultCode, Intent data){
	 	
	}
	</xsl:template>

	<xsl:template name="gen.getintentextras">
	public void getIntentExtras(){
	 
	}
	</xsl:template>

	<xsl:template name="gen.initobjects">
	public void initObjects(){
	 
	}
	</xsl:template>

	<xsl:template name="gen.initviews">
	public void initViews(){
	 
	}
	</xsl:template>

	<xsl:template name="gen.initbroadcastreceivers">
	public void initBroadcastReceivers(){
	 
	}
	</xsl:template>

	<xsl:template name="gen.doRegisterReceivers">
	public void doRegisterReceivers(){
	 
	}
	</xsl:template>

	<xsl:template name="gen.doUnRegisterReceivers">
	public void doUnRegisterReceivers(){
	 
	}
	</xsl:template>
	
	<xsl:template name="gen.resolvedata">
	public void resolveData(){
	 
	}
	</xsl:template>
	

</xsl:stylesheet>