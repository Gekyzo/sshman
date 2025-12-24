#!/bin/zsh
# sshman shell hooks for zsh
# Auto-loads SSH keys when entering directories with .sshman files

# Find .sshman file in current or parent directories
_sshman_find_config() {
    local dir="$PWD"
    while [[ "$dir" != "/" ]]; do
        if [[ -f "$dir/.sshman" ]]; then
            echo "$dir/.sshman"
            return 0
        fi
        dir=${dir:h}
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

# Use zsh's chpwd hook (cleaner than aliasing cd)
autoload -U add-zsh-hook
add-zsh-hook chpwd _sshman_auto_use

# Run on shell startup
_sshman_auto_use
