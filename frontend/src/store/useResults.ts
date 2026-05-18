import { defineStore } from 'pinia';

export const useResultsStore = defineStore('results', {
  state: () => ({
    range: null as null | { table: string; column: string; lowerIndex: number; upperIndex: number },
    rows: [] as any[],
    latestRows: [] as any[],
    decrypted: {} as Record<string, Record<string, string>>
  }),
  actions: {
    setRange(range: any) {
      this.range = range;
    },
    setRows(rows: any[]) {
      this.rows = rows;
      this.decrypted = {};
    },
    setLatestRows(rows: any[]) {
      this.latestRows = rows;
    },
    setDecrypted(recordId: string, values: Record<string, string>) {
      this.decrypted[recordId] = values;
    }
  }
});
