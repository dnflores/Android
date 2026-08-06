# WT7 Companion Bridge v3

Bridge Android local para o Intelbras Allo WT7 IDS9478W.

## Como funciona

Esta versão não precisa de Mac nem de ADB depois de instalada. Ela roda no
mesmo telefone que o Allo Plus, encontra a porta HTTPS local criada pelo app
oficial e envia o comando já validado:

- `set.device.opendoor`
- `door=1`
- `locknumber=1` ou `2`
- `password=SHA256(PIN)`

## Limitação atual

O Allo Plus precisa estar aberto na visualização ao vivo do WT7 para que a
porta P2P local exista. A substituição integral da biblioteca P2P ainda exige
mapear os parâmetros de `createP2PClient()`.

## API

- `GET /health`
- `POST /open/1`
- `POST /open/2`

Todos exigem:

```http
Authorization: Bearer SEU_TOKEN
```

O servidor escuta na porta configurada, padrão `8765`.

## Home Assistant

```yaml
rest_command:
  wt7_porta_social:
    url: "http://IP_DO_ANDROID:8765/open/1"
    method: POST
    headers:
      Authorization: "Bearer SEU_TOKEN"

  wt7_portao_garagem:
    url: "http://IP_DO_ANDROID:8765/open/2"
    method: POST
    headers:
      Authorization: "Bearer SEU_TOKEN"
```

## GitHub Actions

Envie o conteúdo para a raiz do repositório e execute:

`Actions → Build Android APK → Run workflow`

Baixe o artefato `wt7-companion-bridge-debug`.
