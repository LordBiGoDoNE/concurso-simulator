# Produção na VPS Hostinger

Spec `vps-staging-delivery` (#156). Destino:
`https://concurso-simulator.reemsoftworks.com`. IP, chaves e segredos nunca
entram neste repositório; use `<VPS>` como marcador.

## Papéis

| Conta | Uso | Privilégio |
| --- | --- | --- |
| `concurso-dev` | runtime da aplicação (Docker rootless), operador, builds | sem sudo |
| conta administrativa do usuário | nginx, certbot, ufw, permissões do host, reboot | sudo com senha |

O assistente acessa somente `concurso-dev`, a partir do PC do usuário:

```sh
ssh -a -i ~/.ssh/hostingerVPS -o IdentitiesOnly=yes -o ForwardAgent=no concurso-dev@<VPS>
```

A chave do host é verificada (`StrictHostKeyChecking`). Se ela mudar, pare e
confira a impressão digital no console da Hostinger
(`ssh-keygen -lf /etc/ssh/ssh_host_ed25519_key.pub`) antes de aceitar.

Passos com sudo são executados pelo usuário na conta administrativa. Os scripts
abaixo são revisáveis e não pedem nem imprimem segredos.

## Grupo 1 — base do host

Copie a pasta `host/` para a conta administrativa e execute:

```sh
sudo bash host-audit.sh                    # somente leitura
sudo bash fix-vps-control-permissions.sh   # remove escrita de grupo/outros em /opt/vps-control
```

`fix-vps-control-permissions.sh` grava as permissões originais em
`/root/vps-control-permissions-<data>.txt` antes de alterar e não apaga arquivos.
Para reverter um item: `chmod <modo> <caminho>` com os valores do registro.

Na conta `concurso-dev` (o assistente executa):

```sh
bash host/check-project-user.sh
```

Esperado: sem sudo, Docker rootless ativo e habilitado, linger ligado e
serviços do concurso habilitados.

Firewall esperado: entrada somente em 22, 80 e 443 (IPv4/IPv6). As portas
internas (18088, 18090, 18100, 5432) ficam em loopback ou em redes Docker.

### Reboot controlado

Antes: confirmar que não há operação do operador em andamento. O usuário
executa `sudo reboot` na conta administrativa. Depois, o assistente confere:
`check-project-user.sh`, containers saudáveis, `curl` local na porta da web e
nginx respondendo. Nenhum serviço deve exigir intervenção manual.

## Grupo 3 — domínio e TLS

### DNS (Hostinger)

Nas zonas `reemsoftworks.com` e `reemsoftworks.com.br`, TTL 300:

| Nome | Tipo | Valor |
| --- | --- | --- |
| `@` | A / AAAA | IPv4 / IPv6 da VPS (substitui o estacionamento da Hostinger) |
| `concurso-simulator` | A / AAAA | IPv4 / IPv6 da VPS |
| `www` | CNAME | mantido (aponta para `@`) |

Cada alteração é confirmada pelo usuário antes. A Hostinger guarda snapshots da
zona (`dns_snapshots_list`/`restore`) para desfazer.

### nginx e certificado

Copie a pasta `edge/` para a conta administrativa e execute, nesta ordem:

```sh
sudo bash install-edge.sh http <IP-público-do-usuário>   # porta 80 + allowlist de pré-lançamento
sudo bash install-edge.sh cert                           # após o DNS propagar
sudo bash install-edge.sh https
```

- Um certificado `reemsoftworks` cobre os seis nomes; renovação pelo timer
  `reemsoftworks-certificate.timer` com `nginx -t` antes do reload.
- `concurso-simulator.reemsoftworks.com` encaminha para `127.0.0.1:18090` com
  Host fixo, cabeçalhos encaminhados limpos, HSTS de um ano (sem `includeSubDomains`
  nem preload) e log sem query string.
- `*.reemsoftworks.com.br` → 301 para o mesmo host/caminho em `.com`;
  raiz e `www` de `.com` → 302 para o concurso.
- Até a abertura (grupo 7), `/etc/nginx/snippets/concurso-simulator-access.conf`
  permite só o IP do usuário; demais recebem 403. O arquivo fica fora do Git.
- `concurso-public.conf` (acesso antigo por HTTP no IP) e `central.conf` não são
  alterados; a remoção do acesso pelo IP é do corte no grupo 7.
