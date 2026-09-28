# Git Workflow Rules

1. **Manuel Commit (Tavsiye Edilen):** 
   - Proje geliştirilirken her adımda otomatik commit YAPMA.
   - IDE'nin "Undo changes" özelliği kullanıldığında Git ile çakışma olmaması için dosyaları serbest bırak.
   - Sadece kullanıcı "Bunu commit et", "Değişiklikleri kaydet" gibi açık bir talimat verdiğinde `git add .` ve `git commit` işlemlerini yap.

2. **Push on /learn**:
   - Kullanıcı `/learn` yazdığında veya "GitHub'a gönder" dediğinde `git push origin main` işlemini yap.
   - Run `git push origin main` immediately when you see the user typing `/learn`, in addition to executing the standard learn behavior.
