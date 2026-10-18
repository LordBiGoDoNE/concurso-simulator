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

O grupo2 acrescentará Compose/configuração e o grupo3 as operações de dados.
Não use o Compose de desenvolvimento para adotar dados neste ambiente.

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
