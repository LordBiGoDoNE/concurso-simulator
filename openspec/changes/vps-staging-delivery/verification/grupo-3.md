# Grupo 3 — domínio e TLS: evidências

Executado em 2026-11-13.

## DNS (assistente, MCP Hostinger, com autorização do usuário)

Estado anterior, nas duas zonas: `@` A para o estacionamento da Hostinger (TTL 50)
e `www` CNAME para `@`; sem snapshots. Aplicado com validação prévia:
`@` e `concurso-simulator` com A e AAAA para a VPS (TTL 300); `www` mantido.
Propagação conferida em 8.8.8.8 e 1.1.1.1 para os seis nomes, A e AAAA.

## Instalação (usuário, `install-edge.sh` http/cert/https)

- Fase http: allowlist criada (fora do Git); `nginx -t` ok.
- Fase cert: certificado `reemsoftworks` emitido pelo Let's Encrypt (YE1) para os
  seis nomes, válido até 2027-02-11; timer `reemsoftworks-certificate` habilitado.
- Fase https: `nginx -t` ok. `concurso-public.conf` e `central.conf` preservados.

## Verificação externa (assistente)

- TLS válido (`ssl_verify_result` 0) com os seis SANs.
- App do IP liberado: 200, `/api/v1/status` UP, HSTS `max-age=31536000`,
  `nosniff`, `no-referrer`, cookie `Secure; HttpOnly; SameSite=Lax`.
- IP fora da allowlist (a própria VPS): 403 em IPv4 e IPv6.
- `http://concurso-simulator…/x?a=1` → 301 HTTPS com caminho e query.
- `concurso-simulator.reemsoftworks.com.br/x?a=1` → 301 `.com/x?a=1`;
  `www.reemsoftworks.com.br/y` → 301 `www.reemsoftworks.com/y`;
  `reemsoftworks.com.br` → 301 `reemsoftworks.com`.
- Raiz e `www` de `.com` → 302 para o concurso.
- Portas 18088, 18090, 18100, 5432 e 8080 fechadas de fora.
- Após o reboot: HTTPS 200 e timer de renovação agendado.
- `nginx -t` das configs também validado em container descartável (nginx 1.30);
  o host usa 1.24 e nenhuma diretiva exclusiva de versões novas foi usada.

## Limites

O acesso antigo `http://<IP>` continua servindo a release antiga até o corte do
grupo 7. Renovação real ainda não ocorreu (primeira em ~60 dias). O PC não tem
IPv6; o 403 em IPv6 foi conferido a partir da VPS.
