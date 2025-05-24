; Generated Assembly Code
.intel_syntax noprefix
.text

.globl main
main:
    push rbp
    mov rbp, rsp
    sub rsp, 32
    mov [rbp-40], rdi
    mov rax, 5
    mov [rbp-4], rax
    mov rax, 10
    mov [rbp-8], rax
    mov rax, 15
    mov [rbp-12], rax
    mov rsp, rbp
    pop rbp
    ret

