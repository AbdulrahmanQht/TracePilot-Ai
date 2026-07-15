import { mergeProps } from "@base-ui/react/merge-props"
import { useRender } from "@base-ui/react/use-render"
import { cva, type VariantProps } from "class-variance-authority"
import { cn } from "@/lib/utils"

const badgeVariants = cva(
  "group/badge inline-flex w-fit shrink-0 items-center justify-center gap-1 overflow-hidden rounded-none border-2 border-black px-2 py-0.5 text-[10px] font-['Archivo_Black',sans-serif] tracking-[0.06em] uppercase whitespace-nowrap transition-colors focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50 aria-invalid:border-destructive aria-invalid:ring-destructive/20 [&>svg]:pointer-events-none [&>svg]:size-3!",
  {
    variants: {
      variant: {
        default: "bg-primary text-primary-foreground border-black",
        secondary: "bg-secondary text-secondary-foreground border-black",
        outline: "bg-transparent text-foreground border-black",
        muted: "bg-muted text-muted-foreground border-black",
        ghost: "border-transparent bg-transparent hover:bg-muted hover:text-muted-foreground",
        destructive: "bg-destructive text-destructive-foreground border-black",
        link: "border-transparent bg-transparent text-primary underline-offset-4 hover:underline",
        "verdict-contradicted": "bg-[#8B1A1A] text-[#F4F1EA] border-black",
        "verdict-unverified": "bg-[#7A4A28] text-[#F4F1EA] border-black",
        "verdict-complete": "bg-[#1E3A2F] text-[#F4F1EA] border-black",
        "verdict-incomplete": "bg-[#3B2012] text-[#F4F1EA] border-black",
        "status-complete": "bg-[#1E3A2F] text-[#F4F1EA] border-black",
        "status-failed": "bg-[#8B1A1A] text-[#F4F1EA] border-black",
        "status-processing": "bg-[#3B2012] text-[#F4F1EA] border-black",
        "status-pending": "bg-[#E5E0D5] text-[#5C5348] border-black",
        "role-admin": "bg-[#3B2012] text-[#F4F1EA] border-black",
        "role-user": "bg-[#1E3A2F] text-[#F4F1EA] border-black",
        "severity-critical": "bg-[#5C0000] text-[#F4F1EA] border-black",
        "severity-high": "bg-[#8B1A1A] text-[#F4F1EA] border-black",
        "severity-medium": "bg-[#7A4A28] text-[#F4F1EA] border-black",
        "severity-low": "bg-[#1E3A2F] text-[#F4F1EA] border-black",
      },
    },
    defaultVariants: {
      variant: "default",
    },
  }
)

function Badge({
  className,
  variant = "default",
  render,
  ...props
}: useRender.ComponentProps<"span"> & VariantProps<typeof badgeVariants>) {
  return useRender({
    defaultTagName: "span",
    props: mergeProps<"span">(
      { className: cn(badgeVariants({ variant }), className) },
      props
    ),
    render,
    state: { slot: "badge", variant },
  })
}

// eslint-disable-next-line react-refresh/only-export-components
export { Badge, badgeVariants }