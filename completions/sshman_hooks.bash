#!/bin/bash
# sshman shell hooks for bash
# Auto-loads SSH keys when entering directories with .sshman files

# Find .sshman file in current or parent directories
_sshman_find_config() {
    local dir="$PWD"
    while [[ "$dir" != "/" ]]; do
        if [[ -f "$dir/.sshman" ]]; then
            echo "$dir/.sshman"
            return 0
        fi
        dir=$(dirname "$dir")
    done
    return 1
}

# Auto-load SSH key from .sshman file
_sshman_auto_use() {
    local config_file
    config_file=$(_sshman_find_config)

    if [[ -n "$config_file" ]]; then
        local key_name
        key_name=$(head -n 1 "$config_file" | tr -d '\n\r' | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')

        if [[ -n "$key_name" ]]; then
            # Run sshman use in quiet mode and eval the output
            # Suppress errors to avoid breaking cd
            eval "$(sshman use --quiet "$key_name" 2>/dev/null)" 2>/dev/null
        fi
    fi
}

# Wrapper for cd command
_sshman_cd() {
    builtin cd "$@" && _sshman_auto_use
}

# Only override cd if not already defined by sshman
if ! type cd 2>/dev/null | grep -q "is a function"; then
    alias cd='_sshman_cd'
fi

# Run on shell startup
_sshman_auto_use
