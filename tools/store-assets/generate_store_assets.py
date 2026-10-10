#!/usr/bin/env python3
"""Generates the Play Store listing assets in fastlane's `supply` layout.

  fastlane/metadata/android/<locale>/images/featureGraphic.png   (1024x500, no alpha)
  fastlane/metadata/android/<locale>/images/icon.png             (512x512)
  fastlane/metadata/android/<locale>/{title,short_description,full_description}.txt

Requires: rsvg-convert (librsvg) and ImageMagick, plus Noto fonts for every script.
Usage:   python3 tools/store-assets/generate_store_assets.py
Play policy: no promotional wording (price, "free", "sale", rankings, calls to action) in the
graphics, so the artwork only carries the app name and the slogan "here you DON'T buy".
"""
import math
import os
import shutil
import subprocess
import tempfile

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
OUT = os.path.join(ROOT, "fastlane", "metadata", "android")
ICON_SRC = os.path.join(ROOT, "app", "src", "main", "ic_launcher-playstore.png")

# locale: (font, rtl, slogan lead-in, slogan emphasis ("here you DON'T buy"), short, full)
LISTINGS = {
    "en-US": ("Noto Sans", False,
              "Here you", "DON'T buy",
              "A shopping simulator: browse, fill a cart and check out, then keep your money.",
              "Onio is a shopping simulator for people who love the thrill of buying but want to keep their savings.\n\n"
              "Browse a full store, add items to your cart and go through checkout just like in a real shop. A little while later, every order is automatically cancelled and nothing is ever charged.\n\n"
              "See how much money you didn't spend in your Savings tab.\n\n"
              "Important: Onio is a simulation. No real purchase is made, no payment is processed, and card details are never stored or sent anywhere."),
    "zh-CN": ("Noto Sans CJK SC", False,
              "在这里，你", "不买东西",
              "购物模拟器：浏览、加购、结算，钱却原封不动。",
              "Onio 是一款购物模拟器，适合喜欢购物快感、又想守住积蓄的人。\n\n"
              "像在真实商店一样浏览商品、加入购物车并完成结算。过一会儿，每个订单都会自动取消，不会产生任何扣款。\n\n"
              "在“节省”页面查看您没有花掉的金额。\n\n"
              "重要提示：Onio 仅为模拟应用，不会产生真实购买或支付，银行卡信息不会被保存或发送到任何地方。"),
    "hi-IN": ("Noto Sans Devanagari", False,
              "यहाँ आप", "नहीं खरीदते",
              "शॉपिंग सिम्युलेटर: ब्राउज़ करें, कार्ट भरें, चेकआउट करें और पैसे बचाएँ।",
              "Onio एक शॉपिंग सिम्युलेटर है, उन लोगों के लिए जिन्हें खरीदारी का रोमांच पसंद है लेकिन जो अपनी बचत सुरक्षित रखना चाहते हैं।\n\n"
              "असली दुकान की तरह पूरा स्टोर ब्राउज़ करें, कार्ट में सामान जोड़ें और चेकआउट करें। थोड़ी देर बाद हर ऑर्डर अपने-आप रद्द हो जाता है और कोई पैसा नहीं कटता।\n\n"
              "बचत टैब में देखें कि आपने कितना पैसा खर्च नहीं किया।\n\n"
              "ज़रूरी: Onio एक सिमुलेशन है। कोई असली खरीदारी या भुगतान नहीं होता, और कार्ट की जानकारी न कहीं सहेजी जाती है, न भेजी जाती है।"),
    "es-419": ("Noto Sans", False,
               "Aquí", "NO compras",
               "Simulador de compras: explora, arma tu carrito y paga sin gastar tu dinero.",
               "Onio es un simulador de compras para quienes disfrutan la emoción de comprar pero quieren conservar sus ahorros.\n\n"
               "Explora una tienda completa, agrega productos al carrito y completa el pago como en una tienda real. Poco después, cada pedido se cancela automáticamente y nunca se cobra nada.\n\n"
               "Mira cuánto dinero no gastaste en la pestaña Ahorros.\n\n"
               "Importante: Onio es una simulación. No se realiza ninguna compra real, no se procesa ningún pago y los datos de la tarjeta nunca se guardan ni se envían."),
    "fr-FR": ("Noto Sans", False,
              "Ici, vous", "n'achetez PAS",
              "Simulateur d'achats : parcourez, remplissez un panier et payez, sans dépenser.",
              "Onio est un simulateur d'achats pour celles et ceux qui aiment le frisson du shopping mais veulent préserver leurs économies.\n\n"
              "Parcourez une boutique complète, ajoutez des articles au panier et passez commande comme dans un vrai magasin. Peu après, chaque commande est automatiquement annulée et rien n'est jamais débité.\n\n"
              "Consultez dans l'onglet Économies l'argent que vous n'avez pas dépensé.\n\n"
              "Important : Onio est une simulation. Aucun achat réel n'est effectué, aucun paiement n'est traité et les données de carte ne sont ni enregistrées ni transmises."),
    "ar": ("Noto Sans Arabic", True,
           "هنا أنت", "لا تشتري",
           "محاكي تسوق: تصفّح وأضف إلى السلة وأتمم الدفع دون أن تنفق مالك.",
           "Onio محاكي تسوق لمن يحبون متعة الشراء لكنهم يريدون الحفاظ على مدخراتهم.\n\n"
           "تصفّح متجرًا متكاملًا وأضف المنتجات إلى السلة وأتمم عملية الدفع كما في متجر حقيقي. بعد قليل يُلغى كل طلب تلقائيًا ولا يتم خصم أي مبلغ.\n\n"
           "اعرف مقدار المال الذي لم تنفقه من تبويب المدخرات.\n\n"
           "مهم: Onio مجرد محاكاة. لا تتم أي عملية شراء حقيقية ولا تُعالَج أي مدفوعات، ولا يتم حفظ بيانات البطاقة أو إرسالها إلى أي جهة."),
    "bn-BD": ("Noto Sans Bengali", False,
              "এখানে আপনি", "কেনেন না",
              "শপিং সিমুলেটর: ব্রাউজ করুন, কার্ট ভরুন, চেকআউট করুন, টাকা খরচ ছাড়াই।",
              "Onio একটি শপিং সিমুলেটর, যাঁরা কেনাকাটার উত্তেজনা ভালোবাসেন কিন্তু সঞ্চয় রক্ষা করতে চান তাঁদের জন্য।\n\n"
              "আসল দোকানের মতোই পুরো স্টোর ঘুরে দেখুন, কার্টে পণ্য যোগ করুন এবং চেকআউট করুন। কিছুক্ষণ পরে প্রতিটি অর্ডার নিজে থেকেই বাতিল হয়ে যায় এবং কোনো টাকা কাটা হয় না।\n\n"
              "সঞ্চয় ট্যাবে দেখুন আপনি কত টাকা খরচ করেননি।\n\n"
              "গুরুত্বপূর্ণ: Onio একটি সিমুলেশন। কোনো আসল কেনাকাটা বা পেমেন্ট হয় না এবং কার্টের তথ্য কোথাও সংরক্ষণ বা পাঠানো হয় না।"),
    "pt-BR": ("Noto Sans", False,
              "Aqui você", "NÃO compra",
              "Simulador de compras: navegue, monte o carrinho e finalize sem gastar nada.",
              "O Onio é um simulador de compras para quem adora a emoção de comprar, mas quer manter suas economias.\n\n"
              "Navegue por uma loja completa, adicione itens ao carrinho e finalize a compra como em uma loja de verdade. Pouco depois, cada pedido é cancelado automaticamente e nada é cobrado.\n\n"
              "Veja quanto dinheiro você deixou de gastar na aba Economia.\n\n"
              "Importante: o Onio é uma simulação. Nenhuma compra real é feita, nenhum pagamento é processado e os dados do cartão nunca são armazenados nem enviados."),
    "ru-RU": ("Noto Sans", False,
              "Здесь вы", "НЕ покупаете",
              "Симулятор покупок: выбирайте, кладите в корзину, оформляйте заказ — без трат.",
              "Onio — симулятор покупок для тех, кто любит азарт шопинга, но хочет сохранить свои сбережения.\n\n"
              "Просматривайте полноценный магазин, добавляйте товары в корзину и оформляйте заказ, как в настоящем магазине. Через некоторое время каждый заказ автоматически отменяется, и деньги не списываются.\n\n"
              "Во вкладке «Экономия» видно, сколько денег вы не потратили.\n\n"
              "Важно: Onio — это симуляция. Реальных покупок и платежей нет, а данные карты нигде не сохраняются и не передаются."),
    "ur": ("Noto Naskh Arabic", True,
           "یہاں آپ", "خریدتے نہیں",
           "شاپنگ سمیولیٹر: براؤز کریں، کارٹ بھریں، چیک آؤٹ کریں، پیسے خرچ کیے بغیر۔",
           "Onio ان لوگوں کے لیے شاپنگ سمیولیٹر ہے جنہیں خریداری کا لطف پسند ہے مگر جو اپنی بچت محفوظ رکھنا چاہتے ہیں۔\n\n"
           "اصل دکان کی طرح پورا اسٹور براؤز کریں، کارٹ میں چیزیں شامل کریں اور چیک آؤٹ کریں۔ کچھ دیر بعد ہر آرڈر خود بخود منسوخ ہو جاتا ہے اور کوئی رقم نہیں کٹتی۔\n\n"
           "بچت ٹیب میں دیکھیں کہ آپ نے کتنے پیسے خرچ نہیں کیے۔\n\n"
           "اہم: Onio صرف ایک سمیولیشن ہے۔ کوئی حقیقی خریداری یا ادائیگی نہیں ہوتی، اور کارڈ کی معلومات نہ کہیں محفوظ کی جاتی ہیں نہ بھیجی جاتی ہیں۔"),
}

BAG = "M41,45 H67 Q70,45 70.6,48 L73.4,76 Q74,81 69,81 H39 Q34,81 34.6,76 L37.4,48 Q38,45 41,45 Z"
HOLES = " ".join(f"M{cx - 1.7},{cy} a1.7,1.7 0 1,0 3.4,0 a1.7,1.7 0 1,0 -3.4,0 Z" for cx, cy in ((46.5, 50.5), (61.5, 50.5)))
HANDLE = "M44.5,47 C44.5,31 63.5,31 63.5,47"


def check_polygon():
    pts, half = [(45.5, 64.0), (51.5, 70.0), (62.5, 57.0)], 2.4

    def normal(a, b):
        dx, dy = b[0] - a[0], b[1] - a[1]
        length = math.hypot(dx, dy)
        return -dy / length, dx / length

    left, right = [], []
    for i, p in enumerate(pts):
        if i == 0:
            n, k = normal(pts[0], pts[1]), 1.0
        elif i == len(pts) - 1:
            n, k = normal(pts[-2], pts[-1]), 1.0
        else:
            n1, n2 = normal(pts[i - 1], p), normal(p, pts[i + 1])
            nx, ny = n1[0] + n2[0], n1[1] + n2[1]
            length = math.hypot(nx, ny)
            n = (nx / length, ny / length)
            k = 1.0 / max(0.3, n[0] * n1[0] + n[1] * n1[1])
        left.append((p[0] + n[0] * half * k, p[1] + n[1] * half * k))
        right.append((p[0] - n[0] * half * k, p[1] - n[1] * half * k))
    poly = left + right[::-1]
    return "M" + " L".join(f"{x:.2f},{y:.2f}" for x, y in poly) + " Z"


BAG_WITH_HOLES = f"{BAG} {HOLES} {check_polygon()}"


def feature_svg(font, rtl, line1, line2):
    tile, y_tile = 230, 135
    x_tile = 1024 - 96 - tile if rtl else 96
    x_text = x_tile - 44 if rtl else x_tile + tile + 44
    anchor = "start"  # with direction=rtl, "start" is the right edge
    direction = "rtl" if rtl else "ltr"
    return f'''<svg xmlns="http://www.w3.org/2000/svg" width="1024" height="500" viewBox="0 0 1024 500">
<defs>
  <linearGradient id="bg" x1="0" y1="0" x2="1024" y2="500" gradientUnits="userSpaceOnUse">
    <stop offset="0" stop-color="#8A5CFF"/><stop offset="1" stop-color="#4A20C4"/>
  </linearGradient>
  <linearGradient id="bag" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#8A5CFF"/><stop offset="1" stop-color="#4A20C4"/></linearGradient>
</defs>
<rect width="1024" height="500" fill="url(#bg)"/>
<circle cx="{960 if not rtl else 64}" cy="60" r="190" fill="#fff" fill-opacity="0.07"/>
<circle cx="{820 if not rtl else 204}" cy="470" r="130" fill="#fff" fill-opacity="0.06"/>
<circle cx="{520 if not rtl else 504}" cy="-40" r="90" fill="#fff" fill-opacity="0.05"/>
<rect x="{x_tile}" y="{y_tile}" width="{tile}" height="{tile}" rx="56" fill="#fff"/>
<svg x="{x_tile}" y="{y_tile}" width="{tile}" height="{tile}" viewBox="20 22 68 68">
  <path d="{BAG_WITH_HOLES}" fill="url(#bag)" fill-rule="evenodd"/>
  <path d="{HANDLE}" fill="none" stroke="url(#bag)" stroke-width="4" stroke-linecap="round"/>
</svg>
<text x="{x_text}" y="222" font-family="Noto Sans" font-weight="700" font-size="92" fill="#fff" text-anchor="{anchor}" direction="{direction}">Onio</text>
<text x="{x_text}" y="284" font-family="{font}" font-size="34" fill="#fff" text-anchor="{anchor}" direction="{direction}">{line1}</text>
<text x="{x_text}" y="352" font-family="{font}" font-weight="700" font-size="62" fill="#FFD166" text-anchor="{anchor}" direction="{direction}">{line2}</text>
</svg>'''


def main():
    tmp = tempfile.mkdtemp()
    try:
        for locale, (font, rtl, line1, line2, short, full) in LISTINGS.items():
            assert len(short) <= 80, f"{locale}: short description is {len(short)} chars (max 80)"
            assert len(full) <= 4000
            folder = os.path.join(OUT, locale)
            images = os.path.join(folder, "images")
            os.makedirs(images, exist_ok=True)
            svg = os.path.join(tmp, f"{locale}.svg")
            png = os.path.join(tmp, f"{locale}.png")
            with open(svg, "w", encoding="utf-8") as f:
                f.write(feature_svg(font, rtl, line1, line2))
            subprocess.run(["rsvg-convert", "-w", "1024", "-h", "500", svg, "-o", png], check=True)
            # Play requires a 24-bit PNG/JPEG without alpha.
            subprocess.run(["magick", png, "-background", "#4A20C4", "-alpha", "remove", "-alpha", "off",
                            os.path.join(images, "featureGraphic.png")], check=True)
            shutil.copyfile(ICON_SRC, os.path.join(images, "icon.png"))
            for name, text in (("title.txt", "Onio"), ("short_description.txt", short), ("full_description.txt", full)):
                with open(os.path.join(folder, name), "w", encoding="utf-8") as f:
                    f.write(text + "\n")
            print("ok", locale)
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    main()
