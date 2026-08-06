# WT7 Companion Bridge v1

Alternativa sem root, sem `run-as` e sem modificar o Allo Plus.

## Ideia

Este aplicativo Android independente carrega diretamente as duas bibliotecas
oficiais extraídas do split APK:

- `libpairipcore.so`
- `libqv-p2p-v2.so`

A biblioteca exporta JNI público para:

- `createP2PClient`
- `addPortByP2P`
- `getP2PConnectStatus`
- `reconnect`
- `deletePortByP2P`

Portanto, não precisamos ler a memória do Allo Plus. Podemos criar nossa
própria sessão P2P e expor a porta CGI local ao Home Assistant.

## Estado desta versão

- Projeto Android Studio criado.
- Bibliotecas arm64 oficiais incluídas.
- Classes JNI com os nomes exatos incluídas.
- Tela de diagnóstico carrega as bibliotecas e chama
  `setDefaultServiceMask(768)`.

## Próxima etapa

Mapear os 10 parâmetros String e 2 parâmetros int de `createP2PClient`.
A assinatura foi extraída do DEX:

```text
createP2PClient(
  String,String,String,String,String,
  String,String,String,String,String,
  int,int
) -> int
```

Depois:

1. fazer login na nuvem;
2. obter os campos do dispositivo e do MST;
3. chamar `createP2PClient`;
4. chamar `addPortByP2P(deviceId, ...)`;
5. abrir `https://127.0.0.1:<porta>/tdkcgi`;
6. enviar `set.device.opendoor` com `SHA256(PIN)`.

## Abrir no Android Studio

Abra a pasta raiz do projeto e execute em um telefone Android arm64.


## Compilação automática pelo GitHub Actions

Este pacote já inclui `.github/workflows/android.yml`.

1. Envie **o conteúdo desta pasta** para a raiz do repositório.
2. Confirme que na raiz aparecem `settings.gradle.kts`, `build.gradle.kts`,
   `app/` e `.github/workflows/android.yml`.
3. Abra **Actions → Build Android APK → Run workflow**.
4. Baixe o artefato `wt7-companion-bridge-debug`.

O workflow não depende de `gradlew`: instala Gradle 8.10.2 no runner e executa
`gradle :app:assembleDebug`.
