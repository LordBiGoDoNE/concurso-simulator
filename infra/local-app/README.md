# Aplicação local empacotada

Incremento local da etapa7, issue#123. Requer Docker Desktop/Engine com containers
Linux e Compose, Git e Python3 no host. JDK/Node/Python do build ficam nas imagens.
Não contrata hospedagem, configura domínio nem publica a aplicação.

## Construir imagens

Na raiz, com as alterações desejadas já commitadas:

```sh
python scripts/local_app_images.py --origin http://127.0.0.1:8088
```

No Windows também pode usar `py -3 scripts/local_app_images.py`. `--revision` aceita
commit/ref que será resolvido para SHA completo. A ferramenta lê `git archive`,
filtra fontes permitidas e envia um tar diretamente ao Docker. Não envia o workspace,
arquivos ignorados, `.git`, testes, dados editoriais, caches ou backups. Link em path
permitido é recusado. Alterações não commitadas não entram na imagem.

Imagens base fixadas por digest em Dockerfiles/lock; descoberta em2026-10-18 usou
Temurin25,Node24,Python3.14,nginx-unprivileged ePG18.6. Atualização desses pins exige
mudança revisada. O primeiro build baixa imagens/dependências. Os runtimes não
executam Gradle/Vite nem fixtures; web/API usam usuários sem privilégios.

`backend/build/local-app/images.json` registra SHA,origem,tags/IDs,usuários,pins e
hashes dos arquivos/contexto. É ignorado, não contém segredos. IDs identificam a
construção realizada; não se promete determinismo byte a byte. Tag inclui commit
e hash da origem. Porta/origem diferente exige rebuild da web.

O frontend usa API relativa e material em `/material/`. A cópia estática conserva
resoluções públicas do legado separadas do bundle da aplicação. Ela não certifica
ausência de gabaritos no material original nem autoriza exportar capturas privadas.
API/OAuth/callback passam ao backend; nenhum erro dessas rotas vira página da SPA.
Access/error logs do proxy não registram callback/cookies. Status/health dão diagnóstico.
Rejeição de método OAuth conserva o403 vazio canônico do backend,sem redirect/cookie;
não é transformada em JSON nem página da SPA pelo proxy.

O Compose daqui é independente de `infra/compose.yaml`. Não adota seu volume nem
importa a cópia editorial. O primeiro banco começa vazio, com estados de insuficiência
reais; conteúdo real exige escolha posterior de origem/destino/escopo.

## Verificar o filtro de contexto

```sh
python -m unittest discover -s scripts -p test_local_app_images.py
```

Sentinelas sintéticas verificam exclusão e recusa de links/traversal. Inspeção real
das imagens e execução limpa complementam essa proteção; não é detector de segredo
arbitrário inserido em um arquivo de código versionado permitido.

Verificação real das imagens:

```sh
python scripts/check_local_images.py
```

Inspeciona usuários e JAR real, rejeita dependências/fixtures de teste, abre as51
 aulas e exige503/no-store quando a API não existe. Remove somente os containers
 efêmeros cujos IDs criou, sem volumes. Relatório ignorado:backend/build/local-app/image-check.json.

## Iniciar, consultar e parar

```sh
python scripts/local_app.py init
python scripts/local_app.py start
python scripts/local_app.py status
python scripts/local_app.py stop
```

`init` cria `infra/local-app/.local/`, ignorado, com identidade própria e arquivos de
segredo; nunca sobrescreve estado anterior. Projeto `concurso-simulator-app-local`,
volume/rede próprios. API/PG não publicam portas; web somente127.0.0.1:8088.
Origem/porta devem ser as mesmas do build. Colisão falha sem parar outro serviço.
O Docker deve ser local (unix socket/named pipe) e executar containers Linux.

`start` usa IDs das imagens registradas e espera prontidão real dos três serviços.
`stop` preserva volume/segredos; novo `start` retoma os mesmos dados. Não há comando
de remoção no operador. Estado/projeto com recurso de outro proprietário é recusado.
`status` não imprime valores de configuração nem dumps. Falha não é sucesso parcial.

Segredos ficam em `.local/secrets/`: parent0700 e arquivos0644 permitem leitura dos
bind mounts pelo runtime sem root, sem abrir a pasta a outros usuários no Linux.
No Windows, confira a ACL da pasta local; chmod não é garantia de isolamento entre
usuários. Compose secrets não é cofre criptografado; web não recebe nenhum segredo.
Spring lê configtree; PG usa PASSWORD_FILE. Nada é enviado por ARG ou VITE_*.

HTTP exige explicitamente perfil local. Cookie mantém HttpOnly/SameSite=Lax e CSRF;
perfis padrão/produção conservam Secure/HTTPS. Google começa desligado: para testar
opcionalmente, configure os arquivos google_client_id/google_client_secret e mude
google_enabled para true. Callback/frontend já apontam à origem local; cadastre esse
callback exato no client web Google, conforme backend/docs/google-login.md. Reinicie
API após mudança; segredo nunca entra em configuração pública. Startup recusa client
incompleto. OIDC de teste não existe na imagem normal.

Se o banco cair, prontidão retorna503 em prazo finito e o material permanece acessível.
Healthcheck não reinicia automaticamente nem promete disponibilidade. Para diagnóstico
use status/health no Docker Desktop; evite habilitar logs de payload/cookies/SQL bind.
O grupo3 acrescentará backup/restauração com invalidação de sessões restauradas.

Ensaio real e isolado do Compose:

```sh
python scripts/check_local_runtime.py
```

Cria somente projeto `concurso-simulator-app-local-check<id>` e estado em
backend/build/local-app/environments/check-<id>. O cleanup desse teste descarta apenas
esse projeto/volume sintéticos após conferir proprietário. Nunca aceita `.local/`
do operador. Não se aplica ao start/stop normal, que preserva todos os dados.

O ensaio de runtime comprovou origem única,cookie/CSRF/OAuth,zero questões/tentativas
em V10,portas somente na web,colisão sem interrupção,503 com PostgreSQL parado,
material disponível e reinício preservando schema/volume. Relatório ignorado:
backend/build/local-app/runtime-check.json. O teste não simula login Google real.
