export function StockBadge({ stock }: { stock: number }) {
  if (stock === 0) return <span className="badge badge--fout">Uitverkocht</span>
  if (stock <= 3) return <span className="badge badge--warn">Nog {stock} op voorraad</span>
  return <span className="badge badge--ok">{stock} op voorraad</span>
}
