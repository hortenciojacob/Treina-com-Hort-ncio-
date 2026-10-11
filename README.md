# Treina com Hortêncio

Aplicação Android nativa de exercícios físicos, construída em Kotlin e Jetpack Compose.

## Estado

Aplicação Android nativa com treinos adaptados ao tempo, energia, objetivo e equipamento disponível.

Inclui onboarding e perfil local com preferência opcional por movimentos de pé/sentado (sem exercícios no chão),
sessões pausáveis, substituição de exercícios, guias de movimento expansíveis, feedback de esforço e humor,
histórico, metas semanais e mensais, calendário de hábitos que distingue sessões concluídas de progresso
parcial, gráfico de consistência e desafios curtos com
uma experiência de coach em português. As sessões mostram esquemas originais dos movimentos, com guias
passo a passo e cuidados ajustados aos exercícios.
Inclui registo local de água com objetivo pessoal e lembretes Android opcionais, além de conteúdos educativos
de nutrição sem planos clínicos. O registo, o perfil e os lembretes são guardados localmente no dispositivo.
Os dados locais não são incluídos nas cópias de segurança Android; a sequência de consistência mantém-se
até ao dia seguinte para dar espaço a treinar mais tarde. O registo de água permite desfazer a última adição.

As animações são esquemáticas e não substituem demonstração profissional; não há vídeos nem API de exercícios
integrados nesta versão. Os lembretes de hidratação dependem da permissão de notificações e são aproximados.

## Abrir e compilar

1. Abrir a pasta no Android Studio.
2. Aguardar a sincronização do Gradle.
3. Executar a configuração `app`.
4. Para build local: `gradle test assembleDebug`.

Versão atual: 0.2.0 (versionCode 2).

Requisitos: JDK 17, Android SDK 36 e Gradle 8.13. O perfil anterior é migrado automaticamente; a nova
preferência começa desativada para manter os treinos existentes.
