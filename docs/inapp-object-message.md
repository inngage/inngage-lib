# In-App Message v2 — Response Object (`/objectMessage`)

Documentação **agnóstica de plataforma** do objeto de mensagem In-App v2 retornado
pelo backend da Inngage. Serve como contrato de referência para todas as SDKs
(Android, iOS, Web, etc.): descreve o request, o objeto de resposta campo a campo,
os enums aceitos, as regras de parsing e um exemplo completo.

> **Observação:** o objeto é o mesmo para todas as plataformas. Os nomes de tipos
> citados entre parênteses (`InAppMessageV2`, `InAppV2Style`, …) são os modelos de
> domínio da SDK Android e servem apenas como referência de mapeamento.

---

## Endpoint

```
POST https://api.inngage.com.br/v4/message/objectMessage
Content-Type: application/json
```

Chamado sob demanda pelo app consumidor (não está acoplado ao fluxo de push).
Retorna, no máximo, **uma** mensagem In-App para exibir naquele momento.

---

## Request body

| Campo          | Tipo             | Obrigatório | Descrição |
|----------------|------------------|:-----------:|-----------|
| `appId`        | `int` \| `string`| ✅          | Identificador do app na Inngage. Enviado como `string`. Persistido a partir da subscription. |
| `registration` | `string`         | ✅          | Token do dispositivo (FCM/APNs). Persistido durante a subscription. |
| `firstAccess`  | `boolean`        | ✅          | `true` até o **primeiro** fetch bem-sucedido (HTTP 200); depois `false`. Veja [Semântica de `firstAccess`](#semântica-de-firstaccess). |
| `channelId`    | `int`            | ✅          | Canal In-App. Valor fixo: **`6`**. |

> `appId` e `registration` são **pré-requisitos**: se qualquer um estiver ausente,
> a SDK deve abortar o fetch (a subscription precisa ter concluído antes).

**Exemplo de request:**

```json
{
  "appId": "5a1e21eq0v...a717ac41...1q1n",
  "registration": "c9x8...FCM_TOKEN...q1",
  "firstAccess": true,
  "channelId": 6
}
```

---

## Envelopes de resposta aceitos

A SDK deve tolerar **três** formatos de envelope (todos resolvem para o mesmo objeto):

```jsonc
{ "inAppMessage": { /* objeto */ } }   // encapsulado
{ "payload":      { /* objeto */ } }   // encapsulado (legado)
{ /* type, media, ... diretamente na raiz */ } // flat (formato atual de produção)
```

### Quando NÃO há In-App a exibir

A SDK trata como "sem mensagem" (retorno nulo, **sem** renderizar) quando:

- o campo `enabled` está presente e é `false`; **ou**
- o objeto não possui **nem** `media` **nem** `type` (resposta vazia/não relacionada).

---

## Response object

### `InAppMessageV2` (raiz)

| Campo     | Tipo            | Default    | Descrição |
|-----------|-----------------|------------|-----------|
| `enabled` | `boolean`       | `true`     | Se a mensagem deve ser exibida. `false` suprime a exibição. |
| `type`    | `string`        | `"Banner"` | Tipo reportado pelo backend (ex.: `"Banner"`, `"Message"`). Não deve ser vazio. |
| `style`   | `object`        | `{}`       | Estilo visual global do card. Veja [`style`](#style-inappv2style). |
| `media`   | `object`        | `{}`       | Configuração de mídia contendo os slides. Veja [`media`](#media-inappv2media). |

> **Banner vs. Carrossel** é decidido em tempo de renderização pela quantidade de
> slides: **1 item** → banner; **2+ itens** → carrossel (com indicador de dots).

---

### `style` (`InAppV2Style`)

| Campo             | Tipo      | Default     | Descrição |
|-------------------|-----------|-------------|-----------|
| `position`        | `string`  | `"center"`  | Posição do card na tela. |
| `backgroundColor` | `string`  | `""`        | Cor de fundo do card (hex, ex.: `#FFFFFF`). |
| `backgroundImage` | `string?` | `null`      | URL de imagem de fundo. `null`/vazio quando ausente. |
| `borderColor`     | `string`  | `""`        | Cor da borda (hex). Vazio = sem borda. |
| `shadow`          | `boolean` | `false`     | Se o card possui sombra/elevação extra. |
| `titleColor`      | `string`  | `"#000000"` | Cor do título dos slides (hex). |
| `bodyColor`       | `string`  | `"#000000"` | Cor do corpo de texto dos slides (hex). |

---

### `media` (`InAppV2Media`)

Os campos de slides (`items`, `position`, `enabled`) podem vir **diretamente** em
`media` (produção) **ou** aninhados em `media.carousel` (compatibilidade). A SDK
deve aceitar ambos.

| Campo      | Tipo     | Default | Descrição |
|------------|----------|---------|-----------|
| `enabled`  | `boolean`| `false` | Se o carrossel/slides estão habilitados. |
| `position` | `string` | `"TOP"` | Posição do bloco de mídia. |
| `items`    | `array`  | `[]`    | Lista de slides. Veja [`items[]`](#items-inappv2carouselitem). |

---

### `items[]` (`InAppV2CarouselItem`)

Cada elemento representa um slide (banner de slide único ou página do carrossel).

| Campo       | Tipo     | Default  | Descrição |
|-------------|----------|----------|-----------|
| `image`     | `string` | `""`     | URL da imagem do slide. Vazio quando não há imagem. |
| `imageType` | `string` | `"fill"` | Modo de escala da imagem. `"fill"` → recorte central (CENTER_CROP). |
| `content`   | `object` | `{}`     | Título/corpo do slide. Veja [`content`](#itemscontent-inappv2content). |
| `actions`   | `object` | `{}`     | Ação de background + botões do slide. Veja [`actions`](#itemsactions-inappv2actions). |

---

### `items[].content` (`InAppV2Content`)

| Campo   | Tipo     | Default | Descrição |
|---------|----------|---------|-----------|
| `title` | `string` | `""`    | Título do slide. |
| `body`  | `string` | `""`    | Corpo de texto do slide. |

---

### `items[].actions` (`InAppV2Actions`)

| Campo             | Tipo      | Default | Descrição |
|-------------------|-----------|---------|-----------|
| `backgroundClick` | `object?` | `null`  | Ação disparada ao tocar no fundo do slide. Veja [`action`](#objeto-action-inappv2action). |
| `buttons`         | `array`   | `[]`    | Botões do slide. Veja [`buttons[]`](#itemsactionsbuttons-inappv2button). |

---

### `items[].actions.buttons[]` (`InAppV2Button`)

| Campo             | Tipo      | Default | Descrição |
|-------------------|-----------|---------|-----------|
| `text`            | `string`  | `""`    | Rótulo do botão. |
| `style`           | `object`  | `{}`    | Estilo do botão (ver abaixo). |
| `style.backgroundColor` | `string` | `"#000000"` | Cor de fundo do botão (hex). |
| `style.textColor` | `string`  | `"#FFFFFF"` | Cor do texto do botão (hex). |
| `style.hoverColor`| `string`  | `""`    | Cor de hover (hex). |
| `action`          | `object?` | `null`  | Ação do botão. Veja [`action`](#objeto-action-inappv2action). |

---

### Objeto `action` (`InAppV2Action`)

Estrutura comum a `backgroundClick` e ao `action` de cada botão.

| Campo      | Tipo               | Default     | Descrição |
|------------|--------------------|-------------|-----------|
| `type`     | `string` (enum)    | `"dismiss"` | O que fazer ao acionar. Veja [Enum `action.type`](#enum-actiontype). |
| `url`      | `string`           | `""`        | URL/URI de destino. Vazio para `dismiss`. |
| `metadata` | `object<string,string>` | `{}`   | Pares chave-valor arbitrários (usado por `metadata`). |

---

## Enums

### Enum `action.type`

O valor recebido é **normalizado para minúsculas** antes do mapeamento. Valores
não reconhecidos caem em `dismiss`.

| Valor(es) recebidos       | Tipo resolvido | Comportamento (quando a SDK trata a ação) |
|---------------------------|----------------|-------------------------------------------|
| `deeplink`, `deep_link`   | `DEEP_LINK`    | Dispara um deep-link interno no app. |
| `weblink`                 | `WEBLINK`      | Abre a URL no navegador externo. |
| `in_app_url`, `inapp`     | `IN_APP_URL`   | Abre a URL em navegador in-app (ex.: Chrome Custom Tab). |
| `metadata`                | `METADATA`     | Entrega `metadata` ao app — **sem** navegação. |
| *(qualquer outro / ausente)* | `DISMISS`   | Apenas fecha a mensagem. |

### Enum `imageType`

| Valor    | Comportamento |
|----------|---------------|
| `fill`   | Recorte central preenchendo o container (CENTER_CROP). *(default)* |

### `position` (`style.position` / `media.position`)

Strings livres de posicionamento (ex.: `center`, `TOP`). Interpretadas pela camada
de renderização de cada SDK.

---

## Exemplo completo (formato flat de produção)

```json
{
  "type": "Banner",
  "enabled": true,
  "style": {
    "position": "center",
    "backgroundColor": "#FFFFFF",
    "backgroundImage": null,
    "borderColor": "#E0E0E0",
    "shadow": true,
    "titleColor": "#111111",
    "bodyColor": "#444444"
  },
  "media": {
    "enabled": true,
    "position": "TOP",
    "items": [
      {
        "image": "https://cdn.inngage.com.br/inapp/promo.png",
        "imageType": "fill",
        "content": {
          "title": "Bem-vindo!",
          "body": "Aproveite 20% de desconto na sua primeira compra."
        },
        "actions": {
          "backgroundClick": {
            "type": "deeplink",
            "url": "myapp://home",
            "metadata": {}
          },
          "buttons": [
            {
              "text": "Ver ofertas",
              "style": {
                "backgroundColor": "#0066FF",
                "textColor": "#FFFFFF",
                "hoverColor": "#0052CC"
              },
              "action": {
                "type": "in_app_url",
                "url": "https://loja.exemplo.com/ofertas",
                "metadata": {}
              }
            },
            {
              "text": "Agora não",
              "style": {
                "backgroundColor": "#EEEEEE",
                "textColor": "#333333",
                "hoverColor": ""
              },
              "action": {
                "type": "dismiss",
                "url": "",
                "metadata": {}
              }
            }
          ]
        }
      }
    ]
  }
}
```

---

## Regras de parsing (resumo para implementadores de SDK)

1. **Envelope:** procure o objeto em `inAppMessage` → `payload` → raiz (flat), nessa ordem.
2. **Supressão:** retorne "sem In-App" se `enabled == false`, ou se não houver `media` **nem** `type`.
3. **Mídia:** aceite `items`/`position`/`enabled` tanto em `media` quanto em `media.carousel`.
4. **Defaults:** aplique os defaults das tabelas acima para todo campo ausente — o
   backend pode omitir campos opcionais.
5. **`action.type`:** normalize para minúsculas e mapeie conforme o enum; desconhecido → `dismiss`.
6. **Validação mínima antes de renderizar:**
   - `enabled == true`;
   - `type` não vazio;
   - ao menos **um** slide com `image`, `content.title` ou `content.body` — **ou** `style.backgroundImage` presente.

### Semântica de `firstAccess`

- Enviado como `true` até o primeiro fetch com **HTTP 200**.
- O flag só é "consumido" (vira `false`, persistido localmente) **após** o sucesso —
  uma primeira tentativa que falhou ainda conta como `firstAccess` na próxima.

---

## Fluxo resumido na SDK

```
fetch(/objectMessage)  →  parse (envelopes + defaults)  →  validação mínima  →  render
                                                                              ├─ SDK trata a ação (deep-link / browser / in-app), ou
                                                                              └─ app trata a ação (callbacks recebem a lista/clique)
```

Quem trata a ação (SDK ou app) é decidido por um parâmetro na chamada pública de
exibição (ex.: `handledBySdk` na SDK Android), não faz parte do objeto de resposta.
