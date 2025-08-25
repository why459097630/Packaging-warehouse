package com.example.app;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Spec spec = SpecLoader.load(this);
        if (spec == null) {
            Toast.makeText(this, "未找到 spec.json，使用内置示例", Toast.LENGTH_SHORT).show();
            setTitle("NDJC Demo");
            return;
        }

        setTitle(spec.appName);

        TextView tvDesc = findViewById(R.id.tvDesc);
        tvDesc.setText(spec.appDescription == null ? "" : spec.appDescription);

        RecyclerView rv = findViewById(R.id.recyclerScreens);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(new ScreenAdapter(spec));
    }

    // ===== RecyclerView 适配器 =====
    static class ScreenAdapter extends RecyclerView.Adapter<ScreenVH> {
        private final Spec data;
        ScreenAdapter(Spec spec) { this.data = spec; }

        @Override public ScreenVH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_screen, parent, false);
            return new ScreenVH(v);
        }

        @Override public void onBindViewHolder(ScreenVH holder, int position) {
            Spec.Screen s = data.screens.get(position);
            holder.bind(s);
        }

        @Override public int getItemCount() {
            return (data.screens == null) ? 0 : data.screens.size();
        }
    }

    static class ScreenVH extends RecyclerView.ViewHolder {
        private final TextView tvName, tvDesc;
        ScreenVH(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvScreenName);
            tvDesc = itemView.findViewById(R.id.tvScreenDesc);
        }
        void bind(Spec.Screen s) {
            tvName.setText(s.name == null ? "" : s.name);
            tvDesc.setText(s.description == null ? "" : s.description);
        }
    }
}
