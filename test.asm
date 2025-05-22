; Compiler Generated Assembly
; Target: x86-64 Linux

section .text
global _start

main:
    push rbp
    mov rbp, rsp
    sub rsp, 64
    mov rax, 5
    mov [rbp-8], rax
    mov rax, 10
    mov [rbp-16], rax
    mov rax, 15
    mov [rbp-24], rax
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
