"use client"
import * as React from "react"
import { cva, type VariantProps } from "class-variance-authority"
import { cn } from "@/lib/utils"

const labelVariants = cva(
  "flex items-center gap-2 select-none group-data-[disabled=true]:pointer-events-none group-data-[disabled=true]:opacity-50 peer-disabled:cursor-not-allowed peer-disabled:opacity-50",
  {
    variants: {
      variant: {
        default: "font-['Archivo_Black',sans-serif] text-[11px] leading-none tracking-[0.06em] text-foreground uppercase",
        body: "font-['Archivo',sans-serif] text-sm font-medium leading-none text-foreground",
        muted: "font-['JetBrains_Mono',monospace] text-[10px] tracking-[0.08em] text-muted-foreground uppercase",
      },
    },
    defaultVariants: { variant: "default" },
  }
)

function Label({
  className,
  variant,
  ...props
}: React.ComponentProps<"label"> & VariantProps<typeof labelVariants>) {
  return <label data-slot="label" className={cn(labelVariants({ variant }), className)} {...props} />
}

export { Label }