export default function Breadcrumbs({ items }: { items: string[] }) {
  return (
    <div className="flex items-center gap-2 text-xs text-muted-foreground">
      {items.map((x, i) => (
        <span key={x}>
          {i > 0 && "› "}
          {x}
        </span>
      ))}
    </div>
  );
}
