import { Button as ButtonPrimitive } from "@base-ui/react/button"
import { cva, type VariantProps } from "class-variance-authority"
import { cn } from "@/lib/utils"

const buttonVariants = cva(
  "group/button inline-flex shrink-0 items-center justify-center gap-2 rounded-none border-2 border-black bg-clip-padding font-['Archivo_Black',sans-serif] text-sm tracking-tight whitespace-nowrap transition-all outline-none select-none cursor-pointer focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50 disabled:pointer-events-none disabled:opacity-50 aria-invalid:border-destructive aria-invalid:ring-destructive/20 [&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*='size-'])]:size-4",
  {
    variants: {
      variant: {
        default:
          "bg-primary text-primary-foreground shadow-[3px_3px_0px_#0D0D0D] hover:translate-x-[2px] hover:translate-y-[2px] hover:shadow-none",
        secondary:
          "bg-secondary text-secondary-foreground shadow-[3px_3px_0px_#0D0D0D] hover:translate-x-[2px] hover:translate-y-[2px] hover:shadow-none",
        outline:
          "bg-background text-foreground shadow-[3px_3px_0px_#0D0D0D] hover:translate-x-[2px] hover:translate-y-[2px] hover:shadow-none hover:bg-muted",
        ghost:
          "border-transparent bg-transparent text-foreground hover:bg-muted hover:text-foreground",
        destructive:
          "bg-destructive text-destructive-foreground shadow-[3px_3px_0px_#0D0D0D] hover:translate-x-[2px] hover:translate-y-[2px] hover:shadow-none",
        muted:
          "bg-muted text-foreground shadow-[3px_3px_0px_#0D0D0D] hover:translate-x-[2px] hover:translate-y-[2px] hover:shadow-none",
        link: "border-transparent bg-transparent text-primary underline-offset-4 hover:underline shadow-none",
      },
      size: {
        default: "h-9 px-4 py-2 has-data-[icon=inline-start]:pl-3 has-data-[icon=inline-end]:pr-3",
        xs: "h-6 gap-1 px-2 text-xs",
        sm: "h-8 gap-1.5 px-3 text-xs",
        lg: "h-11 px-6 text-base",
        xl: "h-12 px-8 text-base",
        icon: "size-9",
        "icon-xs": "size-6 text-xs",
        "icon-sm": "size-8 text-xs",
        "icon-lg": "size-11",
      },
    },
    defaultVariants: {
      variant: "default",
      size: "default",
    },
  }
)

function Button({
  className,
  variant = "default",
  size = "default",
  ...props
}: ButtonPrimitive.Props & VariantProps<typeof buttonVariants>) {
  return (
    <ButtonPrimitive
      data-slot="button"
      className={cn(buttonVariants({ variant, size, className }))}
      {...props}
    />
  )
}

// eslint-disable-next-line react-refresh/only-export-components
export { Button, buttonVariants }