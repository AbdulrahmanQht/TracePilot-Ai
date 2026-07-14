import * as React from "react"
import { Input as InputPrimitive } from "@base-ui/react/input"
import { cn } from "@/lib/utils"

function Input({ className, type, ...props }: React.ComponentProps<"input">) {
  return (
    <InputPrimitive
      type={type}
      data-slot="input"
      className={cn(
        "h-10 w-full min-w-0 rounded-none border-2 border-black bg-background px-3 py-2 font-['Archivo',sans-serif] text-sm text-foreground outline-none transition-shadow placeholder:text-muted-foreground",
        "focus-visible:shadow-[2px_2px_0px_#0D0D0D]",
        "file:inline-flex file:h-7 file:border-0 file:bg-transparent file:text-sm file:font-medium file:text-foreground",
        "disabled:pointer-events-none disabled:cursor-not-allowed disabled:opacity-50",
        "aria-invalid:border-destructive aria-invalid:bg-destructive/5",
        className
      )}
      {...props}
    />
  )
}

export { Input }