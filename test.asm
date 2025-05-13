; Compiler Generated Assembly
; Target: x86-64 Linux

section .text
global _start

_start:
    ; Call main function
    call main
    ; Exit program
    mov rdi, rax
    mov rax, 60
    syscall
