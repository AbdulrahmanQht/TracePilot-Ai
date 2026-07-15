import * as React from "react"
import { cva, type VariantProps } from "class-variance-authority"
import { cn } from "@/lib/utils"

const alertVariants = cva(
  "relative grid w-full grid-cols-[0_1fr] items-start gap-y-1 rounded-none border-2 border-black px-5 py-4 font-['Archivo',sans-serif] text-sm has-[>svg]:grid-cols-[1.25rem_1fr] has-[>svg]:gap-x-3 [&>svg]:size-4 [&>svg]:translate-y-0.5 [&>svg]:text-current",
  {
    variants: {
      variant: {
        default: "bg-card text-card-foreground",
        info: "bg-[#EAF2EA] text-[#1E3A2F] border-[#1E3A2F] [&>svg]:text-[#1E3A2F]",
        success: "bg-[#D4EDD4] text-[#1A3020] border-[#1E3A2F] [&>svg]:text-[#1E3A2F]",
        warning: "bg-[#FEF3CD] text-[#5C3A00] border-[#B87D2F] [&>svg]:text-[#B87D2F]",
        destructive:
          "bg-[#FEE2E2] text-[#8B1A1A] border-[#8B1A1A] [&>svg]:text-[#8B1A1A] *:data-[slot=alert-description]:text-[#8B1A1A]/80",
        muted: "bg-muted text-muted-foreground border-black",
      },
    },
    defaultVariants: { variant: "default" },
  }
)

function Alert({ className, variant, ...props }: React.ComponentProps<"div"> & VariantProps<typeof alertVariants>) {
  return <div data-slot="alert" role="alert" className={cn(alertVariants({ variant }), className)} {...props} />
}

function AlertTitle({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="alert-title"
      className={cn("col-start-2 font-['Archivo_Black',sans-serif] text-[12px] leading-none tracking-[0.04em]", className)}
      {...props}
    />
  )
}

function AlertDescription({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="alert-description"
      className={cn("col-start-2 text-sm leading-relaxed [&_p]:leading-relaxed", className)}
      {...props}
    />
  )
}

function AlertAction({ className, ...props }: React.ComponentProps<"div">) {
  return <div data-slot="alert-action" className={cn("absolute top-4 right-5", className)} {...props} />
}

export { Alert, AlertTitle, AlertDescription, AlertAction }