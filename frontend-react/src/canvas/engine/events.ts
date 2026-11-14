import type { ChartEvent } from "./types";

export type ChartEventListener = (event: ChartEvent) => void;

export class ChartEventBus {
  private readonly listeners = new Set<ChartEventListener>();

  on(listener: ChartEventListener): () => void {
    this.listeners.add(listener);
    return () => {
      this.listeners.delete(listener);
    };
  }

  off(listener: ChartEventListener): void {
    this.listeners.delete(listener);
  }

  emit(event: ChartEvent): void {
    for (const listener of this.listeners) {
      listener(event);
    }
  }

  clear(): void {
    this.listeners.clear();
  }
}
