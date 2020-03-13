max: slt $t1,$a0,$a1
 beq $t1,$zero,Next
 add $v0,$zero,$a0
 j Exit
Next: add $v0,$zero,$a1
Exit: jr $ra