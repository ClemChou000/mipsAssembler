equal:
caller:
 jal equ
callee:
equ: nori $t0,$a0,0
 addi $t0,$t0,1
 add $t0,$t0,$a1
 bne $t0,$zero,zero
 beq $zero,$zero,exit
zero:
exit: jr $ra