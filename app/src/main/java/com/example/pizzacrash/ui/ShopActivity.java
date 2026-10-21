package com.example.pizzacrash.ui;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.pizzacrash.R;
import com.example.pizzacrash.audio.AudioManager;
import com.example.pizzacrash.data.SaveRepository;
import com.google.android.material.button.MaterialButton;

/**
 * Shop screen (UI_Refs/shop.png): coin balance header + booster packs.
 * Purchases deduct coins through SaveRepository.spendCoins(), which refuses
 * insufficient balances safely, and add to the booster inventory. No real-money
 * purchases (spec §11). Coin pack tab is intentionally absent until approved.
 */
public class ShopActivity extends AppCompatActivity {

    /** Catalog of purchasable items: id, display name, coin price, amount granted. */
    private static final ShopItem[] CATALOG = {
            new ShopItem("hammer",  R.string.booster_hammer,   50, 1),
            new ShopItem("bomb",    R.string.booster_bomb,     80, 1),
            new ShopItem("swap",    R.string.booster_swap,     60, 1),
            new ShopItem("rainbow", R.string.booster_rainbow, 120, 1),
            new ShopItem("starter_pack", R.string.shop_starter_pack, 150, 3),
    };

    private SaveRepository save;
    private TextView textCoins;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop);

        save = new SaveRepository(this);
        textCoins = findViewById(R.id.text_coins);

        findViewById(R.id.button_back).setOnClickListener(v -> finish());

        android.widget.LinearLayout list = findViewById(R.id.shop_list);
        for (ShopItem item : CATALOG) {
            list.addView(buildRow(list, item));
        }
        refreshCoins();
    }

    private MaterialButton buildRow(android.view.ViewGroup parent, ShopItem item) {
        MaterialButton b = new MaterialButton(this, null,
                com.google.android.material.R.style.Widget_MaterialComponents_Button_OutlinedButton);
        b.setText(getString(R.string.label_buy_item, getString(item.nameRes), item.price));
        b.setAllCaps(false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = getResources().getDimensionPixelSize(R.dimen.space_s);
        b.setLayoutParams(lp);
        b.setOnClickListener(v -> buy(item));
        return b;
    }

    private void buy(ShopItem item) {
        if (!save.spendCoins(item.price)) {
            AudioManager.get(this).hapticTap(getWindow().getDecorView());
            Toast.makeText(this, R.string.toast_not_enough_coins, Toast.LENGTH_SHORT).show();
            return;
        }
        AudioManager.get(this).playEffect(AudioManager.SFX_COIN);
        // For the starter pack grant multiple boosters; singles grant one each.
        if ("starter_pack".equals(item.id)) {
            save.addBoosters("hammer", 1);
            save.addBoosters("bomb", 1);
            save.addBoosters("swap", 1);
        } else {
            save.addBoosters(item.id, item.amount);
        }
        Toast.makeText(this, R.string.toast_purchase_ok, Toast.LENGTH_SHORT).show();
        refreshCoins();
    }

    private void refreshCoins() {
        textCoins.setText(String.valueOf(save.getCoins()));
    }

    private static final class ShopItem {
        final String id;
        final int nameRes;
        final int price;
        final int amount;

        ShopItem(String id, int nameRes, int price, int amount) {
            this.id = id;
            this.nameRes = nameRes;
            this.price = price;
            this.amount = amount;
        }
    }
}
