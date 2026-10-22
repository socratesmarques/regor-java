# API Regor

Base: http://localhost:8080. JSON UTF-8. Endpoints protegidos exigem Authorization: Bearer <token>.
Os identificadores são UUIDs. Campos JSON desconhecidos são rejeitados com 400.

## Autenticação e conta

| Método | Caminho | Contrato |
| --- | --- | --- |
| POST | /api/auth/register | email, password (10–72 bytes), displayName, username (3–40 letras, números ou underscore), birthDate (YYYY-MM-DD; idade 18–120) |
| POST | /api/auth/login | email, password |
| GET | /api/users/me | id, email, role, vip, verified, coins |
| PUT | /api/users/me/password | currentPassword, newPassword; revoga JWTs anteriores e exige novo login |

Cadastro e login retornam token, expiresAt e user. JWT HS256 com emissor regor, validade de uma hora e versão de sessão conferida no banco. O segredo vem de REGOR_JWT_SECRET.

## Perfil, fotos e preferências

| Método | Caminho | Contrato |
| --- | --- | --- |
| GET | /api/profiles/me | Perfil próprio, incluindo nascimento e coordenadas privadas |
| PUT | /api/profiles/me | displayName, username, birthDate, location, bio, interests, gender, latitude, longitude |
| GET | /api/profiles/{id} | Perfil público; não retorna data de nascimento ou coordenadas exatas |
| POST | /api/photos | multipart/form-data com campo file; retorna id |
| GET | /api/photos/{id} | JPEG/PNG autenticado |
| DELETE | /api/photos/{id} | Somente o proprietário |
| GET / PUT | /api/preferences | minAge, maxAge, maxDistanceKm, interest, gender, location, matchesNotif, messagesNotif, showDistance, showOnline, discoverable, theme, language |

Fotos: até seis por usuário, 5 MB por arquivo e 24 megapixels. O servidor verifica e recodifica o conteúdo para JPEG/PNG, removendo metadados; armazena os bytes no PostgreSQL.

PUT de perfil e preferências recebe o conjunto completo dos campos editáveis. latitude e longitude podem ser null, sempre juntas. theme aceita escuro/claro; language aceita pt-BR. interests é texto de até 500 caracteres.

## Descoberta e matches

GET /api/discovery aceita q, minAge, maxAge, location, interest, gender, distance (km) e page (inicia em zero). Retorna até 30 perfis. Filtros não enviados usam preferências salvas. Distância usa coordenadas do próprio perfil; sem coordenadas, um filtro explícito de distância retorna 400. Exclui o próprio usuário, perfis ocultos e pessoas já avaliadas.

POST /api/swipes recebe targetId e kind: LIKE, DISLIKE ou SUPERLIKE. Retorna swipeId, matchId e chatId (os dois últimos podem ser null).

LIKE e SUPERLIKE contam como interesse positivo. Quando ambos são positivos, a transação cria exatamente um match, um chat e dois participantes. Locks ordenados por usuário e constraints impedem duplicação sob concorrência. Repetir o swipe atualiza a escolha; não desfaz um match já criado.

GET /api/matches e GET /api/chats aceitam page e retornam até 30 itens com id (match), chatId, profile da outra pessoa e createdAt.

## Mensagens e notificações

| Método | Caminho | Contrato |
| --- | --- | --- |
| GET | /api/chats/{id}/messages?page=0 | Até 50 mensagens; página zero contém as mais recentes, em ordem cronológica |
| POST | /api/chats/{id}/messages | clientId (UUID gerado pelo cliente), content (1–4000 caracteres) |
| GET | /api/notifications?page=0 | Até 50 notificações, mais recentes primeiro |
| PUT | /api/notifications/{id}/read | Marca como lida, somente para o destinatário |

Não existe endpoint público para criar chats ou adicionar participantes. Toda leitura/escrita de mensagem valida o match e a participação. clientId torna o reenvio idempotente; reutilizá-lo com outro conteúdo ou chat retorna 409.

## WebSocket

Conecte a /ws enviando o JWT no cabeçalho Authorization durante o handshake. O JavaFX usa o cliente WebSocket do JDK, que suporta esse cabeçalho.

O servidor envia eventos JSON com type (message ou notification) e data (DTO correspondente), somente após commit da transação e somente aos destinatários autorizados. Não aceita nomes de canais enviados pelo cliente. A escrita de mensagens ocorre por REST; o socket entrega atualizações em tempo real.

O cliente reconecta com atraso progressivo e recarrega o histórico ao reconectar. Tokens expirados/revogados deixam de receber eventos; conexões inválidas são encerradas. A presença online respeita a preferência showOnline.

## Privilegios

role, vip/isVip, verified, coins e permissions não fazem parte de DTOs editáveis. Novas contas recebem USER, VIP falso, verificação falsa e zero moedas. Alterações privilegiadas não são expostas pela API pública.

A entrega WebSocket/presença é local a uma instância do backend; múltiplas instâncias exigem um mecanismo compartilhado de distribuição de eventos.
