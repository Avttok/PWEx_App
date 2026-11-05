# PW Express · App do Motoboy

App Android (Kotlin + Jetpack Compose) que recebe corridas e envia a localização GPS ao servidor (Supabase).

> **Fase atual: piloto do GPS.** Login, estados Offline / Disponível / Em corrida, rastreamento em segundo plano com fila offline.

## Estrutura (modular)

```
app/                 → junta os módulos (Application, MainActivity, ícone)
core/                → compartilhado: configuração, rede (Supabase), sessão, visual PW Express
feature/auth/        → tela de login
feature/tracking/    → rastreamento: serviço GPS, fila offline, sincronização, tela de estado
```

Regra: um módulo `feature` nunca depende de outro `feature`, só do `core`.
Para remover uma funcionalidade, tire a linha do `settings.gradle.kts` e a dependência no `app/build.gradle.kts`.

## Como o rastreamento funciona

| Estado      | O que o GPS faz                                                 |
|-------------|-----------------------------------------------------------------|
| Offline     | Nada. Nenhum dado sai do aparelho.                              |
| Disponível  | Lê a cada 60 s; envia a cada 3 min **ou** quando andar 300 m.   |
| Em corrida  | Alta precisão a cada 45 s (90 s no modo economia).              |

Todo ponto vai primeiro para um banco local no celular e só é apagado depois que o servidor confirma.
Sem internet, os pontos acumulam e são enviados quando o sinal volta.
Os ritmos ficam em `feature/tracking/.../Modo.kt` → `object Intervalos`.

## Configuração

Em `gradle.properties`:

```
pwex.supabaseUrl=https://<seu-projeto>.supabase.co
pwex.supabaseKey=sb_publishable_...
```

Só a chave **publicável**. Nunca coloque a `service_role` / secret key no app.

## Abrir no Android Studio

1. *File → New → Project from Version Control* → cole `https://github.com/Avttok/PWEx_App`.
2. Aguarde o Gradle sincronizar (a primeira vez demora alguns minutos).
   Se o Android Studio sugerir atualizar o Android Gradle Plugin, pode recusar por enquanto.
3. Conecte o celular com **Depuração USB** ligada e clique em ▶ Run.
4. Para receber minhas atualizações: *Git → Pull* (ou `Ctrl+T`).

## Baixar o APK pronto (sem compilar)

A cada atualização no GitHub, o Actions gera o APK automaticamente.
No celular, abra **github.com/Avttok/PWEx_App/releases** (logado), baixe o `.apk` mais recente e instale.
Todas as versões usam a mesma assinatura (`keystore/pwex-debug.jks`), então a nova instala por cima da antiga.

## Preparar o celular (importante em Samsung)

O próprio app mostra um checklist em "Preparar o aparelho". Além dele, nos Samsung:

- *Configurações → Bateria → Limites de uso em segundo plano* → tire o PW Express de "apps em suspensão" e "apps em suspensão profunda", e adicione em **"Apps que nunca entram em suspensão"**.
- *Configurações → Apps → PW Express → Bateria* → **Sem restrições**.
- *Configurações → Apps → PW Express → Permissões → Localização* → **Permitir o tempo todo** e **Usar localização precisa** ligado.
