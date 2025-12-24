#!/bin/bash
# Installation script for sshman shell completion

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BASH_COMPLETION_FILE="$SCRIPT_DIR/sshman_completion.bash"
ZSH_COMPLETION_FILE="$SCRIPT_DIR/_sshman"
BASH_HOOKS_FILE="$SCRIPT_DIR/sshman_hooks.bash"
ZSH_HOOKS_FILE="$SCRIPT_DIR/sshman_hooks.zsh"

echo "sshman Tab Completion Installer"
echo "================================"
echo ""

# Detect shell
if [ -n "$BASH_VERSION" ]; then
    SHELL_TYPE="bash"
elif [ -n "$ZSH_VERSION" ]; then
    SHELL_TYPE="zsh"
else
    echo "Unsupported shell. Please use bash or zsh."
    exit 1
fi

echo "Detected shell: $SHELL_TYPE"
echo ""

# Install based on shell type
if [ "$SHELL_TYPE" = "bash" ]; then
    echo "Installing bash completion..."
    
    # Try system-wide locations first (requires sudo)
    if [ -d "/etc/bash_completion.d" ] && [ -w "/etc/bash_completion.d" ]; then
        cp "$BASH_COMPLETION_FILE" /etc/bash_completion.d/sshman
        echo "✓ Installed to /etc/bash_completion.d/sshman"
    elif [ -d "/usr/local/etc/bash_completion.d" ] && [ -w "/usr/local/etc/bash_completion.d" ]; then
        cp "$BASH_COMPLETION_FILE" /usr/local/etc/bash_completion.d/sshman
        echo "✓ Installed to /usr/local/etc/bash_completion.d/sshman"
    else
        # Fall back to user installation
        mkdir -p ~/.bash_completion.d
        cp "$BASH_COMPLETION_FILE" ~/.bash_completion.d/sshman_completion.bash
        echo "✓ Installed to ~/.bash_completion.d/sshman_completion.bash"
        
        # Add source line to .bashrc if not already there
        BASHRC="$HOME/.bashrc"
        SOURCE_LINE="[ -f ~/.bash_completion.d/sshman_completion.bash ] && source ~/.bash_completion.d/sshman_completion.bash"

        if ! grep -q "sshman_completion.bash" "$BASHRC" 2>/dev/null; then
            echo "" >> "$BASHRC"
            echo "# sshman completion" >> "$BASHRC"
            echo "$SOURCE_LINE" >> "$BASHRC"
            echo "✓ Added source line to $BASHRC"
        else
            echo "  (already sourced in $BASHRC)"
        fi
    fi

    # Install hooks for auto-directory switching
    echo ""
    echo "Installing bash hooks for auto-directory switching..."
    mkdir -p ~/.bash_completion.d
    cp "$BASH_HOOKS_FILE" ~/.bash_completion.d/sshman_hooks.bash
    echo "✓ Installed to ~/.bash_completion.d/sshman_hooks.bash"

    HOOKS_LINE="[ -f ~/.bash_completion.d/sshman_hooks.bash ] && source ~/.bash_completion.d/sshman_hooks.bash"
    if ! grep -q "sshman_hooks.bash" "$BASHRC" 2>/dev/null; then
        echo "# sshman auto-directory switching" >> "$BASHRC"
        echo "$HOOKS_LINE" >> "$BASHRC"
        echo "✓ Added hooks to $BASHRC"
    else
        echo "  (hooks already sourced in $BASHRC)"
    fi
    
    echo ""
    echo "Installation complete!"
    echo "Please restart your shell or run:"
    echo "  source ~/.bashrc"
    
elif [ "$SHELL_TYPE" = "zsh" ]; then
    echo "Installing zsh completion..."
    
    # Determine zsh completion directory
    if [ -d "/usr/local/share/zsh/site-functions" ] && [ -w "/usr/local/share/zsh/site-functions" ]; then
        cp "$ZSH_COMPLETION_FILE" /usr/local/share/zsh/site-functions/_sshman
        echo "✓ Installed to /usr/local/share/zsh/site-functions/_sshman"
    else
        # Use user's local directory
        mkdir -p ~/.zsh/completion
        cp "$ZSH_COMPLETION_FILE" ~/.zsh/completion/_sshman
        echo "✓ Installed to ~/.zsh/completion/_sshman"
        
        # Add fpath line to .zshrc if not already there
        ZSHRC="$HOME/.zshrc"
        FPATH_LINE='fpath=(~/.zsh/completion $fpath)'
        COMPINIT_LINE='autoload -Uz compinit && compinit'

        if ! grep -q "\.zsh/completion" "$ZSHRC" 2>/dev/null; then
            echo "" >> "$ZSHRC"
            echo "# sshman completion" >> "$ZSHRC"
            echo "$FPATH_LINE" >> "$ZSHRC"
            echo "$COMPINIT_LINE" >> "$ZSHRC"
            echo "✓ Added completion setup to $ZSHRC"
        else
            echo "  (already configured in $ZSHRC)"
        fi
    fi

    # Install hooks for auto-directory switching
    echo ""
    echo "Installing zsh hooks for auto-directory switching..."
    mkdir -p ~/.zsh
    cp "$ZSH_HOOKS_FILE" ~/.zsh/sshman_hooks.zsh
    echo "✓ Installed to ~/.zsh/sshman_hooks.zsh"

    HOOKS_LINE="[ -f ~/.zsh/sshman_hooks.zsh ] && source ~/.zsh/sshman_hooks.zsh"
    if ! grep -q "sshman_hooks.zsh" "$ZSHRC" 2>/dev/null; then
        echo "" >> "$ZSHRC"
        echo "# sshman auto-directory switching" >> "$ZSHRC"
        echo "$HOOKS_LINE" >> "$ZSHRC"
        echo "✓ Added hooks to $ZSHRC"
    else
        echo "  (hooks already sourced in $ZSHRC)"
    fi
    
    echo ""
    echo "Installation complete!"
    echo "Please restart your shell or run:"
    echo "  source ~/.zshrc"
fi

echo ""
echo "Test the completion by typing:"
echo "  ./sshman use [TAB]"
