<h1>Inngage - Plataforma de Engajamento Mobile First</h1>

<p>Com a Inngage você cria facilmente comunicações personalizadas e automatizadas de acordo com o contexto de cada usuário, através dos canais: Push Notifications, SMS, In-app Messages, WhatsApp, entre outros!</p>

<h2>Integrando Inngage SDK com aplicativos Android</h2>

<h3>1 - Google Messaging Services & FIREBASE Services</h3>

<p>Antes de implementar a biblioteca, dentro do seu projeto no Android Studio, abra o arquivo build.gradle (Module: app) e adicione as seguintes dependências ao seu projeto.</p>

<h4>1.1 - Implemente essas dependências no arquivo build.gradle (Module: app):</h4>

```cpp
apply plugin: 'com.android.application'
// Adicione esta linha
apply plugin: 'com.google.gms.google-services'

dependencies {
  // Importe o Firebase BoM
  implementation platform('com.google.firebase:firebase-bom:27.1.0')

  // Adicione a dependência para o Firebase SDK para Google Analytics
  // Ao usar o BoM, não especifique versões nas dependências do Firebase
  implementation 'com.google.firebase:firebase-analytics'

  // Adicione as dependências para qualquer outro produto Firebase desejado
  
}
```

<h4>1.2 - Ainda no arquivo build.gradle (Module: app), adicione no final do arquivo o seguinte plugin:</h4>

```cpp
apply plugin: 'com.google.gms.google-services'
```

<h4>1.3 - Em build.gradle (Project: AppName) , Adicione isso:</h4>

```cpp
dependencies {
    // Add this line
    classpath 'com.google.gms:google-services:4.3.2'
  }
```

<h3>2 - Implementação da SDK Inngage (inngage-lib) </h3>

<h4>2.1 - Em build.gradle (Module: app) implemente nossa dependências</h4>

```cpp
dependencies {      
  

       implementation 'com.github.inngage:inngage-lib:3.3.12'

}
```

<h4>2.2 - Em build.gradle (Project: AppName) adicione o link do repositório</h4>

```cpp
dependencies {      
  
       buildscript{
  repositories{
              // adicione somente esta linha dentro de "repositories"
              maven { url 'https://jitpack.io' }
        
  }
}

allprojects {
      repositories {
              // adicione somente esta linha dentro deste outro "repositories"
              maven { url 'https://jitpack.io' }
      }
}

}
```


<h3>Adicionando InngageConstants Interface </h3>

<p>Visando desacoplar as constantes utilizadas em nossa integração, adicione uma nova interface ao seu projeto denominada "InngageConstants"</p>

<h4>3.1- Crie o arquivo como uma interface "InngageConstants" e adicione o seguinte código</h4>

```cpp
interface InngageConstants {

    String inngageAppToken = "APP_TOKEN"; //application token retirado da plataforma Inngage
    String inngageEnvironment = "prod";
    String googleMessageProvider = "FCM";

}
```

<h3>4 - Implementando Importações e Métodos na MainActivity  </h3>

<p>Na MainActivity existem algumas importações e métodos para se implementar para possibilitar o processo de :

subscription - Responsável por fazer a subscrição do usuário na plataforma Inngage;
notification handler - Responsável por fazer a manipulação da notificação quando recebida;</p>

<h4>4.1 - Começaremos importando:</h4>

```cpp
import br.com.inngage.sdk.InngageIntentService;
  import br.com.inngage.sdk.InngageUtils;import br.com.inngage.sdk.InngageIntentService;
  import br.com.inngage.sdk.InngageUtils;
```

<h4>4.2 - Logo após a abertura da classe que implementará esses métodos, adicione essas duas linhas:</h4>

```cpp
private static final String TAG = "INNGAGE";
```

<h4>4.3 - Adicione o seguinte método nesta mesma classe:</h4>

<p>
  <b>A - handleSubscription:</b> Esse método é responsável por fazer a subscrição do usuário na plataforma Inngage. Durante esse processo é possível definir um identificador amigável (identifier) ao usuário (CPF, e-mail, id do banco de dados, etc), e campos personalizados para compor a identidade do usuário de seu app (Nome, E-mail, Data de Nascimento, etc). Aprenda mais sobre campos personalizados clicando aqui.
</p>

<p>Faça a parametrização do "identificador" no método startInit a seu critério (email, username, uuid, id do usuário, cpf, ou mascare esse valor como preferir);</p>
<p>Caso o App não tenha campos personalizados, remova a primeira seção abaixo e o atributo do método startInit.</p>
<p>Certifique-se de definir os customFields na plataforma antes de usá-los aqui.</p>


```cpp
 private void handleSubscription() {
        
        String myInngageAppToken = InngageConstants.inngageAppToken;
        String myInngageEnvironment = InngageConstants.inngageEnvironment;
        String myGoogleMessageProvider = InngageConstants.googleMessageProvider;

        // caso o app possua campos customizáveis, descomente estas linhas, cheque-os na plataforma, substitua-os abaixo e adicione os valores de acordo com a sua aplicação
//        JSONObject jsonCustomField = new JSONObject();
//
//        try {
//
//            jsonCustomField.put("nome", user.name);
//            jsonCustomField.put("email", user.email);
//            jsonCustomField.put("telefone", "");
//            jsonCustomField.put("dataRegistro", "");
//            jsonCustomField.put("dataNascimento", "");
//
//
//      } catch (JSONException e) {
//
//            e.printStackTrace();
//      }
//        // caso possua campos customizáveis, adicione o "jsonCustomField" como na chamada abaixo
//        InngageIntentService.startInit(
//                this,
//                myInngageAppToken,
//                "Identifier", //Seu identificador
//                myInngageEnvironment,
//                myGoogleMessageProvider,
//    jsonCustomField);

        // caso não possua campos customizáveis até então:
        InngageIntentService.startInit(
                this,
                myInngageAppToken,
                "userIdentifier", //Seu identificador
                myInngageEnvironment,
                myGoogleMessageProvider);


        InngageUtils.handleNotification(this, getIntent(), myInngageAppToken, myInngageEnvironment);
```

<h3>5 - Chamando o método</h3>

<p>Depois de adicionar os trechos de código acima, certifique-se de chamar o em onCreate ou onStart:</p>

```cpp
protected void onCreate(Bundle savedInstanceState) 
  {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);
        
       // adicione essa chamada ao onCreate ou onStart da sua activity
        handleSubscription();
      
      
  }
```

<h3>6 - Arquivo AndroidManifest.xml</h3>


<p>
  Depois de implementar a biblioteca, o arquivo de manifesto precisa de algumas permissões e serviços a serem adicionados.</p>


<h4>6.1 - Adicionando Permissões (antes da tag 'application' do AndroidManifest.xml) :</h4>

```cpp

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.READ_PHONE_STATE" />
```

<h4>6.2 - Antes de fechar a tag </ application>, adicione isto:</h4>

<p>
   Ícone de notificação !
é obrigatório colocar o ícone de notificação no repositório drawable sob o nome ic_notification como no exemplo abaixo:

android:resource="@drawable/ic_notification"
</p>

```cpp
<service android:name="br.com.inngage.sdk.PushMessagingService">
         <intent-filter>
         <action android:name="com.google.firebase.MESSAGING_EVENT"/>
         </intent-filter>
</service>

<meta-data
         android:name="com.google.firebase.messaging.default_notification_icon"
         android:resource="@drawable/ic_notification" />

<service android:name="br.com.inngage.sdk.InngageIntentService"
         android:exported="false">
         <intent-filter>
         <action android:name="com.google.firebase.INSTANCE_ID_EVENT"/>
         </intent-filter>
</service>
```

<h3>7 - Configuração abertura do Dialog</h3>

<p>
  Após finalizar toda implementação da SDK Inngage e demais dependências do Android, finalizaremos configurando na plataforma Inngage os parâmetros do seu aplicativo. Nesta sessão deveremos informar o nome do pacote (br.com.suaempresa.seuapp) e a classe que fará o recebimento do Push, no caso dessa implementação MainActivity.
</p>


<h2>Execute seu projeto e registre seu dispositivo</h2>

<p>
  O registro de um dispositivo de teste permitirá que você teste suas mensagens, alterações de variáveis em um dispositivo real.

Para registrar seu dispositivo, primeiro verifique se você está em ambiente de Desenvolvimento. Acesse o App gerado e abra a aba Todo Público em sua conta da Inngage, o Dispositivo deve estar registrado. Você pode olhar todas suas informações em variáveis clicando no ícone (Usuário) e enviar uma notificação clicando no ícone (Avião).

 Nunca use uma chave de desenvolvimento em um aplicativo em produção
O uso de uma chave de desenvolvimento pode não suportar usuários reais (em uma escala de produção). Além disso, todos os dados do usuário serão perdidos, pois não são capturados na análise.

</p>
