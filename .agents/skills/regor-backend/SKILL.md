---
name: regor-backend
description: Desenvolver ou revisar APIs REST, autenticação, autorização e chat WebSocket do backend Java do Regor.
---

# Regor Backend

- Use Java 21+, Spring Boot e Maven; exponha APIs REST e WebSocket para chat.
- Organize o código em controller/service/repository/entity/dto; mantenha controllers pequenos e regras de negócio nos services.
- Use Spring Security, JWT com assinatura e expiração validadas, e BCrypt para hash de senhas.
- Valide entradas com Bean Validation em DTOs específicos por operação; não vincule requisições diretamente a entities.
- Não confie em permissões, identidade ou privilégios enviados pelo frontend; obtenha o usuário do contexto autenticado e autorize cada operação no servidor.
- Autorize acesso, assinatura de canais e envio de mensagens por participante do chat, inclusive via WebSocket.
- Aplique as regras de criação de matches no service, dentro da transação necessária.
- Permita somente campos explicitamente editáveis nos DTOs e mapeamentos; alterações de role, VIP, verified, coins e permissões exigem operações próprias autorizadas no servidor.
