import * as React from "react"
import { Avatar as AvatarPrimitive } from "@base-ui/react/avatar"
import { cva, type VariantProps } from "class-variance-authority"
import { cn } from "@/lib/utils"

const avatarVariants = cva("relative flex shrink-0 overflow-hidden rounded-none border-2 border-black", {
  variants: {
    size: {
      sm: "size-7",
      default: "size-10",
      lg: "size-14",
      xl: "size-20",
    },
  },
  defaultVariants: { size: "default" },
})

const fallbackVariants = cva(
  "flex size-full items-center justify-center font-['Archivo_Black',sans-serif] text-primary-foreground",
  {
    variants: {
      color: {
        primary: "bg-primary",
        secondary: "bg-secondary",
        muted: "bg-muted text-muted-foreground",
      },
      size: {
        sm: "text-[10px]",
        default: "text-sm",
        lg: "text-lg",
        xl: "text-2xl",
      },
    },
    defaultVariants: { color: "secondary", size: "default" },
  }
)

function Avatar({
  className,
  size = "default",
  ...props
}: AvatarPrimitive.Root.Props & VariantProps<typeof avatarVariants>) {
  return (
    <AvatarPrimitive.Root
      data-slot="avatar"
      data-size={size}
      className={cn(avatarVariants({ size }), className)}
      {...props}
    />
  )
}

function AvatarImage({ className, ...props }: AvatarPrimitive.Image.Props) {
  return (
    <AvatarPrimitive.Image
      data-slot="avatar-image"
      className={cn("aspect-square size-full object-cover", className)}
      {...props}
    />
  )
}

function AvatarFallback({
  className,
  color,
  size,
  ...props
}: AvatarPrimitive.Fallback.Props & VariantProps<typeof fallbackVariants>) {
  return (
    <AvatarPrimitive.Fallback
      data-slot="avatar-fallback"
      className={cn(fallbackVariants({ color, size }), className)}
      {...props}
    />
  )
}

function AvatarBadge({ className, ...props }: React.ComponentProps<"span">) {
  return (
    <span
      data-slot="avatar-badge"
      className={cn(
        "absolute right-0 bottom-0 z-10 inline-flex items-center justify-center rounded-none border-2 border-black bg-primary text-primary-foreground select-none",
        "group-data-[size=sm]/avatar:size-2.5 group-data-[size=default]/avatar:size-3 group-data-[size=lg]/avatar:size-3.5",
        className
      )}
      {...props}
    />
  )
}

function AvatarGroup({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="avatar-group"
      className={cn("group/avatar-group flex -space-x-2 *:data-[slot=avatar]:ring-2 *:data-[slot=avatar]:ring-background", className)}
      {...props}
    />
  )
}

function AvatarGroupCount({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="avatar-group-count"
      className={cn(
        "relative flex size-10 shrink-0 items-center justify-center rounded-none border-2 border-black bg-muted text-sm text-muted-foreground",
        className
      )}
      {...props}
    />
  )
}

export { Avatar, AvatarImage, AvatarFallback, AvatarGroup, AvatarGroupCount, AvatarBadge }