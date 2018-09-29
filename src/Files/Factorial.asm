Fac$$:
    .long 0
    .long Fac$ComputeFac
    .text
    .global asm_main

asm_main:
    movl $10, %eax
    pushl %eax
    pushl $4
    call mjmalloc
    addl $4, %esp
    movl (%esp), %ecx
    leal Fac$$, %ebx
    movl %ebx, (%eax)
    movl %eax, %ecx
    movl (%eax), %eax
    addl $4, %eax
    movl (%eax), %eax
    call *%eax
    addl $4, %esp
    movl (%esp), %ecx
    pushl %eax
    call put
    addl $4, %esp
    movl (%esp), %ecx
    ret
Fac$ComputeFac:
    pushl %ebp
    movl %esp, %ebp
    subl $4, %esp
    pushl %ecx
    # parameter num
    movl 8(%ebp), %eax
    pushl %eax
    movl $12, %eax
    popl %edx
    cmpl %eax, %edx
    jl L2
    movl $0, %eax
    jmp L3
L2:
    movl $1, %eax
L3:
    cmpl $0, %eax
    je L0
    movl $1, %eax
    # local var num_aux
    movl %eax, -4(%ebp)
    jmp L1
L0:
    # parameter num
    movl 8(%ebp), %eax
    pushl %eax
    movl $2, %eax
    popl %edx
    imul %edx, %eax
    # local var num_aux
    movl %eax, -4(%ebp)
L1:
    jmp L5
L4:
    movl $30, %eax
    # local var num_aux
    movl %eax, -4(%ebp)
    movl $40, %eax
    # parameter num
    movl %eax, 8(%ebp)
L5:
    # parameter num
    movl 8(%ebp), %eax
    pushl %eax
    movl $10, %eax
    popl %edx
    cmpl %eax, %edx
    jl L6
    movl $0, %eax
    jmp L7
L6:
    movl $1, %eax
L7:
    cmpl $0, %eax
    jne L4
    # local var num_aux
    movl -4(%ebp), %eax
    addl $5, %esp
    movl %ebp, %esp
    popl %ebp
    ret
