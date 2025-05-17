; Compiler Generated Assembly
; Target: x86-64 Linux

section .text
global _start

main:
    push rbp
    mov rbp, rsp
    sub rsp, 64
.end_main:
    mov rsp, rbp
    pop rbp
    ret

_start:
    ; Call main function
    call main
    ; Exit program
    mov rdi, rax
    mov rax, 60
    syscall
