# PostgreSQL local

Requer Docker Engine/Desktop com Docker Compose v2. Imagem **PostgreSQL 18.6**, compartilhada com os testes Testcontainers.

Na raiz do repositório:

```sh
cp infra/.env.example infra/.env
# Edite infra/.env e preencha POSTGRES_PASSWORD com uma senha local.
docker compose -f infra/compose.yaml up -d --wait
docker compose -f infra/compose.yaml exec postgres psql -U concurso -d concurso_simulator
docker compose -f infra/compose.yaml stop
docker compose -f infra/compose.yaml start --wait
```

O Compose lê `infra/.env` automaticamente. O banco escuta somente em `127.0.0.1`, na porta 5432 por padrão. Ajuste `POSTGRES_PORT` se já houver outro banco nessa porta.

O volume `postgres_data` é montado em `/var/lib/postgresql`, conforme o layout da imagem PostgreSQL 18. Parar, reiniciar ou executar `docker compose -f infra/compose.yaml down` **preserva os dados**. Não use `down -v`: ele apaga o volume.

Alterar a senha no `.env` não muda a senha de um banco já inicializado. Faça a alteração pelo PostgreSQL e atualize a configuração do backend, sem apagar o volume.

Não migre volumes de versões antigas diretamente para essa imagem. O volume deste projeto deve ser criado com PostgreSQL 18; uma migração de dados antigos requer procedimento próprio.
