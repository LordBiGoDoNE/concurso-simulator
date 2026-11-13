#!/bin/bash
# Remove escrita de grupo/outros em /opt/vps-control, sem apagar nada.
# Executar na conta administrativa:  sudo bash fix-vps-control-permissions.sh
# Grava antes um registro das permissões originais para permitir reverter.
set -euo pipefail
[[ $EUID -eq 0 ]] || { echo 'Execute com sudo.' >&2; exit 1; }

target=/opt/vps-control
[[ -d $target && ! -L $target ]] || { echo "$target ausente ou link; nada feito." >&2; exit 1; }
[[ $(stat -c %U "$target") == root ]] || { echo "$target não pertence a root; pare e confira." >&2; exit 1; }

record=/root/vps-control-permissions-$(date +%Y%m%d%H%M%S).txt
umask 077
find "$target" ! -type l -printf '%m %u:%g %p\n' > "$record"
echo "Permissões originais registradas em $record"

# Links simbólicos não são seguidos; somente arquivos e diretórios reais.
find "$target" ! -type l -exec chmod go-w {} +

remaining=$(find "$target" ! -type l -perm /022 | wc -l)
echo "Itens ainda graváveis por grupo/outros: $remaining"
[[ $remaining -eq 0 ]]
